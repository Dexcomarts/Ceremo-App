import hashlib
import io
from pathlib import Path
import tarfile
import tempfile
import unittest
from unittest.mock import MagicMock, patch
import warnings
import zipfile

from ci_tools import install, unpack_tool


class ToolInstallTest(unittest.TestCase):
    def test_extracts_only_the_expected_binary(self):
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, 'w') as archive:
            archive.writestr('../unexpected.txt', 'not extracted')
            archive.writestr('tool.exe', b'reviewed executable')
        data = buffer.getvalue()
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / 'tool.exe'
            unpack_tool(data, hashlib.sha256(data).hexdigest(), 'tool.exe', target)
            self.assertEqual(target.read_bytes(), b'reviewed executable')
            self.assertFalse((Path(directory).parent / 'unexpected.txt').exists())
            with self.assertRaisesRegex(ValueError, 'checksum'):
                unpack_tool(data, '0' * 64, 'tool.exe', target)

    def test_rejects_missing_duplicate_and_link_binaries(self):
        for member in ('other', 'tool'):
            buffer = io.BytesIO()
            with tarfile.open(fileobj=buffer, mode='w:gz') as archive:
                entry = tarfile.TarInfo(member)
                entry.type = tarfile.SYMTYPE
                entry.linkname = '/outside'
                archive.addfile(entry)
            data = buffer.getvalue()
            with tempfile.TemporaryDirectory() as directory, self.assertRaises(ValueError):
                unpack_tool(data, hashlib.sha256(data).hexdigest(), 'tool', Path(directory) / 'tool')

        buffer = io.BytesIO()
        with warnings.catch_warnings(), zipfile.ZipFile(buffer, 'w') as archive:
            warnings.simplefilter('ignore', UserWarning)
            archive.writestr('tool', b'first')
            archive.writestr('tool', b'second')
        data = buffer.getvalue()
        with tempfile.TemporaryDirectory() as directory, self.assertRaises(ValueError):
            unpack_tool(data, hashlib.sha256(data).hexdigest(), 'tool', Path(directory) / 'tool')

    def test_release_timeout_uses_only_the_pinned_asset_fallback_and_same_checksum(self):
        response = MagicMock()
        response.__enter__.return_value.read.return_value = b'archive'
        with patch('ci_tools.platform.system', return_value='Windows'), \
                patch('ci_tools.platform.machine', return_value='AMD64'), \
                patch.dict('os.environ', {'GITHUB_PATH': ''}), \
                patch('ci_tools.urllib.request.urlopen', side_effect=[TimeoutError(), response]) as request, \
                patch('ci_tools.unpack_tool') as unpack:
            install('actionlint')
        fallback = request.call_args_list[1].args[0]
        self.assertEqual(fallback.full_url, 'https://api.github.com/repos/rhysd/actionlint/releases/assets/384924919')
        self.assertEqual(fallback.get_header('Accept'), 'application/octet-stream')
        self.assertIsNone(fallback.get_header('Authorization'))
        self.assertEqual(unpack.call_args.args[:3], (b'archive',
            '6e7241b51e6817ea6a047693d8e6fed13b31819c9a0dd6c5a726e1592d22f6e9', 'actionlint.exe'))


if __name__ == '__main__':
    unittest.main()
