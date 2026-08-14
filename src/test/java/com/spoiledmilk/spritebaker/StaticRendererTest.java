package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;
import org.junit.jupiter.api.Test;

class StaticRendererTest {
    @Test
    void rendersGeneratedNeutralMeshDeterministicallyOnTransparency() {
        ModelDefinition fixture = new ModelDefinition();
        fixture.id = 9000;
        fixture.vertexCount = 4;
        fixture.vertexX = new int[] {-40, 40, 0, 0};
        fixture.vertexY = new int[] {0, 0, 0, -80};
        fixture.vertexZ = new int[] {-30, -30, 40, 0};
        fixture.faceCount = 4;
        fixture.faceIndices1 = new int[] {0, 0, 1, 2};
        fixture.faceIndices2 = new int[] {2, 1, 2, 0};
        fixture.faceIndices3 = new int[] {1, 3, 3, 3};
        fixture.faceColors = new short[] {5000, 5000, 5000, 5000};

        NpcDefinition530 npc = new NpcDefinition530(1);
        StaticRenderer renderer = new StaticRenderer();
        BufferedImage first = renderer.render(List.of(fixture), npc);
        BufferedImage second = renderer.render(List.of(fixture), npc);

        assertEquals(StaticRenderer.WIDTH, first.getWidth());
        assertEquals(StaticRenderer.HEIGHT, first.getHeight());
        assertEquals(0, first.getRGB(0, 0));
        int[] firstPixels = first.getRGB(0, 0, first.getWidth(), first.getHeight(), null, 0, first.getWidth());
        int[] secondPixels = second.getRGB(0, 0, second.getWidth(), second.getHeight(), null, 0, second.getWidth());
        assertArrayEquals(firstPixels, secondPixels);
        assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel -> (pixel >>> 24) == 255));
    }
}
