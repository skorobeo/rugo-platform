package ru.rugo.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogOptionsTest {

    @Test
    void combinedCategoriesContainEquipmentAndMaterials() {
        assertTrue(CatalogOptions.CATEGORIES.containsAll(CatalogOptions.EQUIPMENT_CATEGORIES));
        assertTrue(CatalogOptions.CATEGORIES.containsAll(CatalogOptions.MATERIAL_TYPES));
        assertEquals(
                CatalogOptions.EQUIPMENT_CATEGORIES.size() + CatalogOptions.MATERIAL_TYPES.size(),
                CatalogOptions.CATEGORIES.size());
    }

    @Test
    void boomCategoriesAreSubsetOfEquipmentCategories() {
        assertTrue(CatalogOptions.EQUIPMENT_CATEGORIES.containsAll(CatalogOptions.BOOM_CATEGORIES));
    }

    @Test
    void offerTypesContainKnownDirections() {
        assertTrue(CatalogOptions.OFFER_TYPES.contains("equipment"));
        assertTrue(CatalogOptions.OFFER_TYPES.contains("materials"));
        assertTrue(CatalogOptions.OFFER_TYPES.contains("repair"));
        assertFalse(CatalogOptions.OFFER_TYPES.contains("unknown"));
    }

    @Test
    void priceUnitsAndTypesMatch() {
        assertEquals(3, CatalogOptions.PRICE_UNITS.size());
        assertEquals(3, CatalogOptions.PRICE_TYPES.size());
    }

    @Test
    void limitsArePositive() {
        assertTrue(CatalogOptions.MAX_PHOTOS > 0);
    }
}
