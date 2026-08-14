package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RenderAnimation530DecoderTest {
    @Test
    void decodesGeneratedNeutralMovementIdentifiers() {
        byte[] fixture = {
            1, 0, 10, 0, 11,
            6, 0, 12,
            53,
            0
        };

        RenderAnimation530 result = new RenderAnimation530Decoder().decode(4, fixture);

        assertEquals(4, result.id);
        assertEquals(10, result.standingAnimation);
        assertEquals(11, result.walkingAnimation);
        assertEquals(12, result.runningAnimation);
    }
}
