from pathlib import Path
R=Path(__file__).resolve().parents[1]
editor=(R/'src/main/java/net/foundations/pl4/client/DisplayEditorScreen.java').read_text()
elements=(R/'src/main/java/net/foundations/pl4/core/DisplayElements.java').read_text()
partitem=(R/'src/main/java/net/foundations/pl4/PartItem.java').read_text()
tool=(R/'src/main/java/net/foundations/pl4/ToolItem.java').read_text()
host=(R/'src/main/java/net/foundations/pl4/HostBlock.java').read_text()
chrome=(R/'src/main/java/net/foundations/pl4/core/EditorChrome.java').read_text()
guide=(R/'src/main/java/net/foundations/pl4/client/GuideScreen.java').read_text()
search=(R/'src/main/java/net/foundations/pl4/client/GuideSearchBox.java').read_text()
checks={
 'toolbar shared core geometry':'EditorChrome.toolRect(i)' in editor and 'EditorChrome.toolAt(p.x(),p.y(),TOOLS.length)' in editor,
 'toolbar background plane five':'0xE6080B0D,5)' in editor,
 'toolbar border plane six':'canvas.outline(r,accent,6)' in editor,
 'toolbar glyph plane seven':'false,7)' in editor,
 'hover accent plane eight':'accent,8)' in editor,
 'four corner handles':'Corner.NW,EditorChrome.Corner.NE,EditorChrome.Corner.SW,EditorChrome.Corner.SE' in editor,
 'corner brackets not solid squares':'h.bottom()-2' in editor and 'h.right()-2' in editor and '0x401DE7FF' in editor,
 'large handle rule':'HANDLE_SIZE=10' in chrome,
 'corner resize dispatch':'EditorChrome.resize(start,resizeCorner' in editor,
 'context help retained':('drawHelp(canvas)' in editor) or ('drawHudHelp(g)' in editor and 'RMB back' in editor),
 'colored key caps':'"E",0xFF63C7FF' in editor and ('"X",0xFFFF6B6B' in editor or '"DEL",0xFFFF6B6B' in editor) and '"G",0xFF75E56B' in editor,
 'eight shallow planes':'Math.clamp(layer,0,8)*.01/16.0' in elements,
 'normal breaks use stackable path':'PartItem.stack(part,level.registryAccess())' in (R/'src/main/java/net/foundations/pl4/HostEntity.java').read_text(),
 'operator uses config-preserving path':'PartItem.savedStack(part,level.registryAccess())' in tool,
 'normal default drops omit custom data':'needsEscrowPayload' in partitem and 'new ItemStack(FoundationsPL4.PART_ITEMS.get(part.kind).get())' in partitem,
 'runtime item identity stripped':'for(String key:PartItemDataRules.VOLATILE_KEYS)out.remove(key)' in partitem,
 'saved face normalized':'out.putInt("face",Direction.DOWN.ordinal())' in partitem,
 'technical binder keeps PL4 palette':'Calculator-style technical binder' in guide and '0xFF62C7D6' in guide and 'TAB_SHORT' in guide,
 'guide search moved to reference header':'int sy=layout.compact()?b.bottom()-48:b.y()+48' in guide and 'Search chapters...' in search,
}
failed=[k for k,v in checks.items() if not v]
for k,v in checks.items():print(('PASS ' if v else 'FAIL ')+k)
if failed:raise SystemExit('R13 source guard failed: '+', '.join(failed))
print(f'PASS R13 source guards: {len(checks)} of {len(checks)}.')
