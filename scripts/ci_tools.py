"""Install reviewed CI executables locally, checking the complete release archive."""

import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import platform
import stat
import tarfile
import urllib.request
import urllib.error
import zipfile

LIMIT = 100_000_000


def unpack_tool(data, checksum, binary, target):
    if len(data) > LIMIT or hashlib.sha256(data).hexdigest() != checksum:
        raise ValueError('Release archive checksum does not match the reviewed pin')
    if zipfile.is_zipfile(io.BytesIO(data)):
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            entries = [entry for entry in archive.infolist() if entry.filename == binary]
            if (len(entries) != 1 or entries[0].file_size > LIMIT or
                    stat.S_ISLNK(entries[0].external_attr >> 16)):
                raise ValueError('Expected exactly one bounded executable')
            payload = archive.read(entries[0])
    else:
        with tarfile.open(fileobj=io.BytesIO(data), mode='r:gz') as archive:
            entries = [entry for entry in archive.getmembers() if entry.name in (binary, './' + binary)]
            if len(entries) != 1 or not entries[0].isfile() or entries[0].size > LIMIT:
                raise ValueError('Expected exactly one regular executable')
            with archive.extractfile(entries[0]) as stream:
                payload = stream.read(LIMIT + 1)
    if not payload or len(payload) > LIMIT:
        raise ValueError('Executable is empty or oversized')
    target = Path(target)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(payload)
    target.chmod(0o755)


def install(name):
    root = Path(__file__).resolve().parent.parent
    pins = json.loads((root / '.github/ci-tools.json').read_text())
    system = platform.system().lower()
    if platform.machine().lower() not in ('amd64', 'x86_64') or system not in ('linux', 'windows'):
        raise ValueError('Reviewed binaries are available for Linux/Windows x86-64 only')
    pin = pins[name][system]
    binary = name + ('.exe' if system == 'windows' else '')
    try:
        with urllib.request.urlopen(pin['url'], timeout=60) as response:
            data = response.read(LIMIT + 1)
    except (urllib.error.URLError, TimeoutError):
        if 'assetUrl' not in pin:
            raise
        request = urllib.request.Request(pin['assetUrl'], headers={'Accept': 'application/octet-stream'})
        with urllib.request.urlopen(request, timeout=60) as response:
            data = response.read(LIMIT + 1)
    target = root / 'build/tools/pinned' / binary
    unpack_tool(data, pin['sha256'], binary, target)
    if os.environ.get('GITHUB_PATH'):
        with open(os.environ['GITHUB_PATH'], 'a', encoding='utf-8') as paths:
            paths.write(str(target.parent) + '\n')
    print(f'Installed checksum-verified {name}: {target}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('tools', nargs='+', choices=('actionlint', 'zizmor', 'gitleaks'))
    for tool in parser.parse_args().tools:
        install(tool)
