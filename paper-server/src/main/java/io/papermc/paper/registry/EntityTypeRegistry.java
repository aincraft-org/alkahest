package io.papermc.paper.registry;

import dev.mintychochip.customentity.CustomEntities;
import java.util.Map;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.VanillaEntityType;
import org.jspecify.annotations.NullMarked;

/**
 * Server tag-aware merged view of vanilla and custom entity types.
 *
 * <p>Custom entity definitions retain their carrier-backed identity and are not inserted into the
 * native entity registry. Tag operations are delegated to the vanilla registry by the shared
 * {@link PaperCatalogRegistry} implementation.
 */
@NullMarked
final class EntityTypeRegistry extends PaperCatalogRegistry<EntityType> {

    @SuppressWarnings({"unchecked", "rawtypes"})
    EntityTypeRegistry(final Registry<VanillaEntityType> vanilla) {
        super(() -> vanilla, () -> (Map) CustomEntities.catalog().asMap());
    }
}
