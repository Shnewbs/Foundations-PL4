"""Reviewed 0.2a feature migration onto existing 26.1.2/26.3 native ports.

This is not a general Minecraft port generator. It preserves target-specific
rendering, transfer adapters, test harnesses and metadata. Plan before writing.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import re

JAVA = Path('src/main/java/net/foundations/pl4')
GUIDE = Path('src/main/resources/assets/foundations_pl4/guide/en_us.json')


def replace_once(text: str, old: str, new: str) -> str:
    if text.count(old) != 1:
        raise ValueError('Missing or ambiguous migration anchor: ' + old[:90])
    return text.replace(old, new, 1)


def between(text: str, start: str, end: str) -> str:
    if text.count(start) != 1 or text.count(end) != 1:
        raise ValueError('Missing or ambiguous section: ' + start[:80])
    begin = text.index(start)
    finish = text.index(end, begin)
    return text[begin:finish]


def server_api(text: str) -> str:
    return text.replace('.serverLevel()', '.level()').replace('.dimension().location()', '.dimension().identifier()')


def plan(source: Path, target: Path, minecraft: str) -> dict[Path, str]:
    if minecraft not in {'26.1.2', '26.3'}:
        raise ValueError('Only the two existing 26.x native ports are supported')
    read = lambda p: (target / p).read_text(encoding='utf-8')
    main = lambda p: (source / p).read_text(encoding='utf-8')
    old_status = json.loads(read('BUILD_STATUS.json'))
    if old_status['minecraft'] != minecraft or old_status['version'] != '0.1b':
        raise ValueError('Expected the independently ported 0.1b target baseline')
    if "version = '0.2a'" not in main('build.gradle'):
        raise ValueError('Expected 0.2a reference source')
    changes = {}
    changes[JAVA / 'ComponentLinks.java'] = server_api(main(JAVA / 'ComponentLinks.java'))

    name = JAVA / 'NetworkEngine.java'
    start = '    /** Physical data network only:'
    end = '    public static void ensureCurrent('
    changes[name] = replace_once(read(name), end, between(main(name), start, end) + end)

    name = JAVA / 'PLPackets.java'
    text = read(name)
    start = '    public record StorageRequest('
    end = '    private record Rate('
    text = replace_once(text, between(text, start, end), between(main(name), start, end))
    text = replace_once(text, 'event.registrar("4")', 'event.registrar("5")')
    anchor = '    private static void sendOpen(ServerPlayer player,HostEntity host,Part p,String error,boolean reply){\n'
    text = replace_once(text, anchor, anchor + '        if(ComponentLinks.supported(p))ComponentLinks.refresh(player,host,p);\n')
    start = '                case "link_add"'
    end = '                case "remove_link"'
    text = replace_once(text, end, between(main(name), start, end) + end)
    end = '        if(p.kind.display())DisplayNetworks.layoutEdited(h,p);'
    text = replace_once(text, end, '        if(Set.of("link_query","link_page").contains(packet.field)){h.setChanged();reply(player,anchorHost,anchorPart);return;}\n' + end)
    changes[name] = text

    name = JAVA / 'ToolItem.java'
    text = read(name)
    start = '            if(host!=null&&part!=null&&link!=null&&(part.kind==Kind.ARRAY||part.kind==Kind.ENTITY_NODE||part.kind.receiver())){'
    end = '        return use(level,player,c.getHand())'
    updated = between(main(name), start, end).replace('player.displayClientMessage(', 'player.sendOverlayMessage(').replace('),true);', '));')
    changes[name] = replace_once(text, between(text, start, end), updated)

    name = JAVA / 'WirelessStorage.java'
    text = server_api(main(name))
    text = replace_once(text, 'import net.neoforged.neoforge.items.IItemHandler;', 'import net.foundations.pl4.compat.TransferAdapters.IItemHandler;')
    text = replace_once(text, 'import net.neoforged.neoforge.capabilities.Capabilities;\n', '')
    text = replace_once(text, 'world.getCapability(Capabilities.ItemHandler.BLOCK,link.pos(),link.side())', 'net.foundations.pl4.compat.TransferAdapters.items(world,link.pos(),link.side())')
    text = text.replace('player.displayClientMessage(', 'player.sendOverlayMessage(').replace('),true);', '));')
    if minecraft == '26.3':
        text = replace_once(text, 'player.drop(extracted,false)', 'player.drop(extracted,false,net.minecraft.util.Prediction.SERVER_ONLY)')
    changes[name] = text

    name = JAVA / 'client/PartScreen.java'
    text = read(name)
    text = replace_once(text, '!p.statements.equals(part.statements);', '!p.statements.equals(part.statements)||(ComponentLinks.supported(p)&&(!p.targetChoices.equals(part.targetChoices)||p.targetPage!=part.targetPage));')
    start = '            }else if(part.kind==Kind.ARRAY||part.kind==Kind.ENTITY_NODE||part.kind.receiver()){'
    end = '            button("Apply fields"'
    changes[name] = replace_once(text, between(text, start, end), between(main(name), start, end))

    name = JAVA / 'client/WirelessStorageScreen.java'
    text = main(name).replace('GuiGraphics', 'GuiGraphicsExtractor').replace('renderBackground', 'extractBackground').replace('g.drawString(', 'g.text(')
    text = text.replace('net.neoforged.neoforge.network.PacketDistributor', 'net.neoforged.neoforge.client.network.ClientPacketDistributor').replace('PacketDistributor.sendToServer', 'ClientPacketDistributor.sendToServer')
    text = re.sub(r'\b(tag|row)\.getString\(("[^"]+")\)', r'\1.getString(\2).orElse("")', text)
    text = re.sub(r'\b(tag|row)\.getBoolean\(("[^"]+")\)', r'\1.getBoolean(\2).orElse(false)', text)
    text = re.sub(r'\b(tag|row)\.getInt\(("[^"]+")\)', r'\1.getInt(\2).orElse(0)', text)
    text = re.sub(r'\b(tag|row)\.getLong\(("[^"]+")\)', r'\1.getLong(\2).orElse(0L)', text)
    text = text.replace('tag.getList("storageRows",Tag.TAG_COMPOUND)', 'tag.getList("storageRows").orElseGet(ListTag::new)')
    text = text.replace('list.getCompound(i)', 'list.getCompound(i).orElseGet(CompoundTag::new)')
    changes[name] = text

    name = JAVA / 'R16GameTests.java'
    start = '    private record NetworkStorageFixture('
    addition = main(name)[main(name).index(start):].rsplit('}', 1)[0]
    addition = server_api(addition).replace('@GameTest(', '@PortGameTest(')
    text = read(name)
    if not text.rstrip().endswith('}'):
        raise ValueError('Unexpected native test class ending')
    changes[name] = text.rstrip()[:-1] + addition + '}\n'
    if changes[name].count('@PortGameTest(') != 29:
        raise ValueError('Expected 29 storage/automation native fixtures')

    for name in ['verify_r7.py', 'verify_r8.py', 'verify_r9.py', 'verify_r10.py']:
        path = Path('tools') / name
        text = replace_once(read(path), 'registrar("4")', 'registrar("5")')
        changes[path] = text.replace('No packet/schema change intended', '0.2a storage query/sort requires protocol 5')
    path = Path('tools/verify_r16.py')
    text = replace_once(read(path), ".count('@PortGameTest(')==21", ".count('@PortGameTest(')==29")
    changes[path] = text.replace('twenty-one transfer and automation native tests', 'twenty-nine transfer, automation and storage native tests')

    changes[Path('build.gradle')] = replace_once(read('build.gradle'), "version = '0.1b'", "version = '0.2a'")
    book = json.loads(read(GUIDE)); previous = book['edition']
    book['edition'] = replace_once(previous, '0.1b', '0.2a')
    field = replace_once(read('docs/FIELD_GUIDE.md'), previous, book['edition'])
    updates = json.loads(main('docs/releases/0.2a-guide.json'))
    for patch in updates:
        matches = [s for c in book['chapters'] if c['id'] == patch['chapter'] for s in c['sections'] if s['heading'] == patch['heading']]
        if len(matches) != 1:
            raise ValueError('Missing guide section: ' + patch['heading'])
        section = matches[0]
        if section['body'] != patch['body']:
            field = replace_once(field, section['body'], patch['body'])
            section['body'] = patch['body']
    changes[GUIDE] = json.dumps(book, indent=2, ensure_ascii=False) + '\n'
    changes[Path('docs/FIELD_GUIDE.md')] = field
    count = sum(changes.get(p.relative_to(target), p.read_text(encoding='utf-8')).count('@PortGameTest(') for p in (target / JAVA).glob('*GameTests.java'))
    old_status.update(version='0.2a', release_channel='alpha', payload_protocol='5', expected_native_tests=count, client_visual_acceptance='PENDING', further_api_testing_required=True, release_notes='docs/releases/0.2a.md')
    changes[Path('BUILD_STATUS.json')] = json.dumps(old_status, indent=2) + '\n'
    changes[Path('docs/releases/0.2a.md')] = f"""# Foundations PL4 {minecraft} - 0.2a

Alpha port of the 0.2a network storage and validated component-selection update.
**Further API testing is still required.** Use Java 25 and the exact Minecraft
version named in this release; a 26.3 JAR is not a 26.1.2 JAR.

Wireless Storage combines inventories on the bound physical data network, with
name/resource-ID search, quantity/name sorting, component-aware variants,
withdrawals across sources and offhand deposits past full inventories. Every
request revalidates the tool, binding, ownership, loaded targets and filters;
single-use tokens reject replay. Visual-only links do not grant storage access.
Receivers and Entity Nodes gain server-validated selection and held-link actions.

The port uses its own transfer adapters, NBT accessors, client extraction API,
item-drop signature and native test-instance registry. The pipeline must pass
compilation, offline regressions, standalone checks and all {count} registered
native fixtures before publishing. These fixtures include eight newly ported
network-storage and component-link scenarios. Installed optional-mod API testing,
real-client rendering and live multiplayer acceptance remain pending.

Back up worlds before upgrading. Payload protocol is 5; use matching client and
server builds. Save schema remains 2. Rebind older Wireless Storage reader/display
bindings to a Node or Transfer Node. Optional mods are not required for core PL4
storage, displays, native inventory access or configuration. This does not certify
unavailable third-party APIs or provide a universal power conversion guarantee.

Other requested legacy ports, including experimental 1.6.4, are separate work;
this release does not claim they are complete. Minecraft 1.7.10 is excluded.
"""
    changes[Path('README.md')] = f"""# Foundations PL4 - Minecraft {minecraft} - 0.2a

Standalone NeoForge alpha port. Java 25. Further API testing is still required.

Network-wide Wireless Storage with search/sorting, cross-inventory withdrawals,
offhand deposits, permission revalidation and component-safe variants. Receiver
and Entity Node settings provide validated component selection.

Build with `bash gradlew build runGameTestServer`. The release workflow verifies
all {count} registered native tests, offline regressions and standalone packaging
before publishing the target-specific JAR, source JAR, source ZIP and checksums.
A successful compile is not installed-mod or real-client acceptance.

Use only the JAR for this exact Minecraft version. Both client and server must
use protocol 5 builds. Back up worlds before upgrading. No Sonar Core or
MCMultiPart runtime dependency is required. See [release notes](docs/releases/0.2a.md)
and [field guide](docs/FIELD_GUIDE.md).
"""
    path = Path('.github/workflows/port-26.yml')
    workflow = read(path)
    workflow = replace_once(workflow, 'import pathlib, re, shutil, hashlib, os', 'import pathlib, re, shutil, hashlib, os, subprocess')
    workflow = replace_once(workflow, '          hashes=[]', """          archive=out/f'FoundationsPL4-{os.environ.get("GITHUB_REF_NAME", "").split("/")[-1]}-{version}-source.zip'
          subprocess.run(['git','archive','--format=zip','--output='+str(archive),'HEAD'],check=True)
          hashes=[hashlib.sha256(archive.read_bytes()).hexdigest()+'  '+archive.name]""")
    workflow = replace_once(workflow, 'build/libs/*.jar build/libs/SHA256SUMS.txt', 'build/libs/*.jar build/libs/*.zip build/libs/SHA256SUMS.txt')
    anchor = '          if ! gh release view "$RELEASE_TAG" >/dev/null 2>&1; then'
    workflow = replace_once(workflow, anchor, """          if gh release view "$RELEASE_TAG" >/dev/null 2>&1; then
            git fetch origin "refs/tags/$RELEASE_TAG:refs/tags/$RELEASE_TAG"
            test "$(git rev-parse "$RELEASE_TAG^{commit}")" = "$GITHUB_SHA" || { echo 'Existing release has different source; use a new revision.'; exit 1; }
          else""")
    changes[path] = workflow
    return changes


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('target', type=Path)
    parser.add_argument('minecraft', choices=['26.1.2', '26.3'])
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    try:
        changes = plan(args.source.resolve(), args.target.resolve(), args.minecraft)
        for name, text in changes.items():
            print(name)
            if not args.check:
                dest = args.target / name
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_text(text, encoding='utf-8')
    except (OSError, ValueError, KeyError) as error:
        parser.exit(1, 'Port migration rejected: ' + str(error) + '\n')


if __name__ == '__main__':
    main()
