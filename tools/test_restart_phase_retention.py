import json,tempfile,unittest
from pathlib import Path
from verify_legacy_item_restart import retain_phase_report,digest,require_legacy_decode_placements,require_legacy_decode_terrain_observations,require_legacy_decode_terrain_log_match,retain_decode_placement_failure,require_legacy_decode_gravity_guards

class PhaseRetentionTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.work=self.root/'work';self.logs=self.root/'logs'
        self.work.mkdir();self.logs.mkdir();self.log=self.logs/'native.log';self.log.write_text('native failure\n')
        self.source=self.work/'item-decode-missing-restart-write.json'
    def retain(self):return retain_phase_report(self.work,self.logs,'item-decode-missing-restart','write',71,1,self.log)
    def observation(self):return json.loads((self.logs/'item-decode-missing-restart-write-process.json').read_text())
    def test_failed_phase_preserves_exact_raw_bytes(self):
        raw=b'{ "passed": false, "assertions": [], "placements": [{"x":64,"tile_class":null}] }\n'
        self.source.write_bytes(raw);self.assertIs(self.retain()['passed'],False)
        saved=self.logs/self.source.name;self.assertEqual(saved.read_bytes(),raw)
        self.assertEqual(self.observation(),dict(phase='write',pid=71,exit_code=1,log_sha256=digest(self.log),report_present=True,report_sha256=digest(saved)))
    def test_invalid_json_is_retained_before_parse_failure(self):
        self.source.write_bytes(b'{broken')
        with self.assertRaises(json.JSONDecodeError):self.retain()
        self.assertEqual((self.logs/self.source.name).read_bytes(),b'{broken')
        self.assertIs(self.observation()['report_present'],True)
    def test_absent_report_preserves_process_facts(self):
        with self.assertRaisesRegex(ValueError,'Missing restart phase report'):self.retain()
        self.assertIs(self.observation()['report_present'],False);self.assertIsNone(self.observation()['report_sha256'])
    def test_success_report_is_preserved_without_granting_clearance(self):
        self.source.write_text('{"passed":true,"assertions":["one"]}')
        self.assertEqual(self.retain(),dict(passed=True,assertions=['one']))
        self.assertEqual(self.observation()['exit_code'],1)
class NativeTerrainObservationsTests(unittest.TestCase):
    def good(self,x):
        return dict(x=x,chunk_x=x>>4,chunk_z=2,chunk_loaded=True,
                    host_exists=True,host_block=0,
                    overhead_falling_blocks=0,live_falling_entities=0)
    def report(self,rows):
        return dict(suite='decode-rewrite',terrain_before_cleanup=rows)
    def test_all_four_native_columns_accepted(self):
        rows=[self.good(x) for x in (64,69,80,85)]
        self.assertIsNone(require_legacy_decode_terrain_observations(self.report(rows),'write','1.6.4'))
        self.assertIsNone(require_legacy_decode_terrain_observations({},'verify','1.6.4'))
        self.assertIsNone(require_legacy_decode_terrain_observations({},'write','1.12.2'))
    def test_missing_duplicate_and_wrong_chunk_rejected(self):
        rows=[self.good(x) for x in (64,69,80,85)]
        for bad in (rows[:3],rows[:3]+[rows[2]], [dict(rows[0],chunk_x=5)]+rows[1:],
                    [dict(rows[0],chunk_loaded=False)]+rows[1:],
                    [dict(rows[0],host_exists=False)]+rows[1:]):
            with self.subTest(bad=bad),self.assertRaises(ValueError):
                require_legacy_decode_terrain_observations(self.report(bad),'crash-write','1.6.4')
    def test_negative_and_boolean_counters_rejected(self):
        rows=[self.good(x) for x in (64,69,80,85)]
        for key,value in [('host_block',True),('overhead_falling_blocks',-1),
                          ('live_falling_entities',True),('chunk_z',False)]:
            bad=[dict(rows[0],**{key:value})]+rows[1:]
            with self.subTest(key=key),self.assertRaises(ValueError):
                require_legacy_decode_terrain_observations(self.report(bad),'write','1.6.4')

    def trace(self,rows,actor=False):
        lines=[]
        for row in rows:
            x=row['x']
            lines.append('PL4 DECODE TERRAIN BEFORE '+json.dumps(row,separators=(',',':')))
            if x in (69,85):
                start=(64,69) if x==69 else (80,85)
                if x==85 and actor:lines.append('PL4 DECODE GRAVITY ADVERSARY x=80 spawned=true removed=true')
                lines.extend('PL4 DECODE GRAVITY GUARD x=%d removed_blocks=0 removed_entities=0 ceiling=true' % n for n in start)
        return '\\n'.join(lines)
    def test_native_terrain_log_exact_rows_and_order(self):
        rows=[self.good(x) for x in (64,69,80,85)]
        self.assertIsNone(require_legacy_decode_terrain_log_match(self.report(rows),self.trace(rows,True),'write','1.6.4'))
        self.assertIsNone(require_legacy_decode_terrain_log_match({},'','verify','1.6.4'))
        self.assertIsNone(require_legacy_decode_terrain_log_match({},'','write','1.12.2'))
    def test_native_terrain_log_missing_duplicate_or_mutated_rejected(self):
        rows=[self.good(x) for x in (64,69,80,85)]
        good=self.trace(rows,True)
        first='PL4 DECODE TERRAIN BEFORE '+json.dumps(rows[0],separators=(',',':'))
        for bad in (good.replace(first,'',1),good+'\\n'+first,
                    good.replace('"chunk_x":5','"chunk_x":4',1),
                    good.replace('"host_block":0','"host_block":13',1),
                    good.replace('x=80 spawned=true','x=80 spawned=false')):
            with self.subTest(bad=bad),self.assertRaises(ValueError):
                require_legacy_decode_terrain_log_match(self.report(rows),bad,'crash-write','1.6.4')
    def test_native_terrain_log_reorder_and_late_snapshot_rejected(self):
        rows=[self.good(x) for x in (64,69,80,85)]
        good=self.trace(rows,True)
        wrong=rows[:];wrong[0],wrong[1]=wrong[1],wrong[0]
        altered=good.replace(
            'PL4 DECODE TERRAIN BEFORE '+json.dumps(rows[0],separators=(',',':')),
            'PL4 DECODE TERRAIN BEFORE '+json.dumps(wrong[0],separators=(',',':')),1)
        for bad in (altered,good.replace(
                'PL4 DECODE TERRAIN BEFORE '+json.dumps(rows[0],separators=(',',':')),
                '',1)+'\\nPL4 DECODE TERRAIN BEFORE '+json.dumps(rows[0],separators=(',',':'))):
            with self.subTest(bad=bad),self.assertRaises(ValueError):
                require_legacy_decode_terrain_log_match(self.report(rows),bad,'write','1.6.4')

class NativeDecodePlacementAdmissionTests(unittest.TestCase):
    def good(self,x):
        return dict(x=x,y=200,z=32,stage='complete',placement_flags=2,
            set_block_changed=True,expected_block=3501,actual_block=3501,
            tile_class='net.foundations.pl4.legacy.LegacyTile',tile_invalid=False,
            block_exists_after=True)
    def test_exact_hosts_accepted_without_retries(self):
        report=dict(suite='decode-raw',placements=[self.good(64),self.good(80),dict(x=200)])
        self.assertIsNone(require_legacy_decode_placements(report,'write','1.6.4'))
        self.assertIsNone(require_legacy_decode_placements(report,'verify','1.6.4'))
        self.assertIsNone(require_legacy_decode_placements(report,'write','1.12.2'))
    def test_gravel_overwrite_rejected_even_after_set_block_reports_success(self):
        other=self.good(80);other['stage']='placement_assertion'
        other['actual_block']=13;other['tile_class']=None
        report=dict(suite='decode-raw',placements=[self.good(64),other])
        with self.assertRaisesRegex(ValueError,'x=80'):
            require_legacy_decode_placements(report,'write','1.6.4')
    def test_failed_placement_retains_json_and_reports_native_stage(self):
        with tempfile.TemporaryDirectory() as temporary:
            logs=Path(temporary);log=logs/'native.log';log.write_text('native x80 failed\n')
            bad=dict(self.good(80),stage='placement_assertion',actual_block=13,
                     tile_class=None,neighbor_nonair=[dict(dx=0,dy=1,dz=0,block=13)],
                     falling_entities_near=1)
            report=dict(suite='decode-rewrite',placements=[self.good(64),bad])
            try:
                require_legacy_decode_placements(report,'write','1.6.4')
            except ValueError as error:
                path=retain_decode_placement_failure(logs,'item-decode-rewrite-restart',
                                                     'write',123,1,log,report,error)
            else: self.fail('Native gravel overwrite was incorrectly accepted')
            evidence=json.loads(path.read_text())
            self.assertEqual(evidence['placements'][1]['actual_block'],13)
            self.assertEqual(evidence['placements'][1]['falling_entities_near'],1)
            self.assertIn('"actual_block":13',evidence['failure'])
            self.assertEqual(evidence['log_sha256'],digest(log))
    def test_native_gravity_guard_requirements(self):
        line=lambda x: 'PL4 DECODE GRAVITY GUARD x=%d removed_blocks=0 removed_entities=1 ceiling=true' % x
        log='\n'.join(line(x) for x in (64,69,80,85))
        self.assertIsNone(require_legacy_decode_gravity_guards(log,'write','1.6.4'))
        self.assertIsNone(require_legacy_decode_gravity_guards('','verify','1.6.4'))
        self.assertIsNone(require_legacy_decode_gravity_guards('','write','1.12.2'))
        for broken in (log.replace('ceiling=true','ceiling=false',1),
                       log.replace('x=85','x=80'),log.replace('x=69','x=68'),
                       log.replace(' removed_entities=1',' removed_entities=-1',1),
                       log.split('\n',1)[0]):
            with self.subTest(broken=broken),self.assertRaises(ValueError):
                require_legacy_decode_gravity_guards(broken,'crash-write','1.6.4')
    def test_rewrite_requires_real_gravel_actor_cleanup(self):
        guards='\n'.join('PL4 DECODE GRAVITY GUARD x=%d removed_blocks=0 removed_entities=%d ceiling=true' %
                         (x,1 if x==80 else 0) for x in (64,69,80,85))
        marker='PL4 DECODE GRAVITY ADVERSARY x=80 spawned=true removed=true'
        self.assertIsNone(require_legacy_decode_gravity_guards(guards+'\n'+marker,'write','1.6.4','decode-rewrite'))
        self.assertIsNone(require_legacy_decode_gravity_guards(guards,'write','1.6.4','decode-raw'))
        self.assertIsNone(require_legacy_decode_gravity_guards('', 'verify','1.6.4','decode-rewrite'))
        for bad in (guards,guards+'\n'+marker+'\n'+marker,
                    guards+'\n'+marker.replace('removed=true','removed=false'),
                    guards.replace('x=80 removed_blocks=0 removed_entities=1','x=80 removed_blocks=0 removed_entities=0')+'\n'+marker):
            with self.subTest(bad=bad),self.assertRaisesRegex(ValueError,'native rewrite falling-gravel'):
                require_legacy_decode_gravity_guards(bad,'crash-write','1.6.4','decode-rewrite')

    def test_missing_duplicate_or_wrong_flags_rejected(self):
        for rows in ([self.good(64)],[self.good(64),self.good(64)],
                     [dict(self.good(64),placement_flags=3),self.good(80)],
                     [dict(self.good(64),tile_invalid=True),self.good(80)]):
            with self.subTest(rows=rows),self.assertRaises(ValueError):
                require_legacy_decode_placements(dict(suite='decode-raw',placements=rows),'crash-write','1.6.4')

if __name__=='__main__':unittest.main()
