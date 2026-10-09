package net.foundations.pl4.legacy;

/**
 * Optional PL4-owned extension point for legacy providers without Forge capabilities.
 * Sides are 0..5 (down/up/north/south/west/east); null means denied, with no fallback.
 * Ports use the target's documented PL4 energy unit, not an inferred EU/RF/J conversion.
 * Providers must return a stable backing-store identity, obey simulation, and use the server thread.
 */
public interface LegacyEnergyAccess {
    ConservingEnergy.Port pl4EnergyPort(int side);
}
