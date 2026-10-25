"""Shared release maturity and explicit RC clearance policy."""
import re


def release_type(version):
    text = version.lower()
    if re.search(r"(?:^|[.\-_])(?:dev|snapshot|nightly)(?:[.\-_]|$)", text):
        raise ValueError("Development snapshots are not qualified releases")
    if re.search(r"beta|(?:^|[.\-_])rc\d*(?:[.\-_]|$)|\db(?:[.\-_]|$)", text):
        return "beta"
    if re.search(r"alpha|preview|experimental|(?:^|[.\-_])port(?:[.\-_]|$)|\da(?:[.\-_]|$)", text):
        return "alpha"
    return "release"


def is_rc(version):
    return bool(re.search(r"(?:^|[.\-_])rc\d*(?:[.\-_]|$)", version, re.I))


def require_publication(status, minecraft=None, version=None):
    if not isinstance(status, dict):
        raise ValueError("Missing exact-source build status")
    actual = status.get("version", "")
    if not actual or (version is not None and actual != version):
        raise ValueError("Build status and release version disagree")
    if minecraft is not None and status.get("minecraft") != minecraft:
        raise ValueError("Build status and exact Minecraft target disagree")
    maturity = release_type(actual)
    if status.get("release_channel", "").lower() == "development":
        raise ValueError("Development channel is not publishable")
    if is_rc(actual) and status.get("rc_publication_cleared") is not True:
        raise ValueError("RC publication has not been explicitly cleared")
    return maturity
