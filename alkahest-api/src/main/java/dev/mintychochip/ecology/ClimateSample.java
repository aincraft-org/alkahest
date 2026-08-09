package dev.mintychochip.ecology;

/** Climate at a location: continuous humidity and biome-category region. */
public record ClimateSample(double humidityValue, String region) {
    public ClimateSample {
        humidityValue = Math.max(0.0, Math.min(1.0, humidityValue));
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("region required");
        }
    }
}