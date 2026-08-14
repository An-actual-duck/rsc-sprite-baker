package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BatchValidationTest {
    @Test void validatesSupportedProjectSchemas(@TempDir Path directory)throws Exception{Path one=directory.resolve("one.json"),two=directory.resolve("two.json"),bad=directory.resolve("bad.json");Files.writeString(one,"{\"schemaVersion\":1}");Files.writeString(two,"{\"schemaVersion\":2}");Files.writeString(bad,"{\"schemaVersion\":3}");assertEquals(1,BatchProcessor.validateProjectSchema(one));assertEquals(2,BatchProcessor.validateProjectSchema(two));assertThrows(java.io.IOException.class,()->BatchProcessor.validateProjectSchema(bad));}
    @Test void requiresCorrectDimensionsTransparencyAndVisiblePixels(@TempDir Path directory)throws Exception{Path valid=directory.resolve("valid.png");BufferedImage image=new BufferedImage(4,3,BufferedImage.TYPE_INT_ARGB);image.setRGB(1,1,0xffff00ff);ImageIO.write(image,"PNG",valid.toFile());BatchProcessor.ImageFacts facts=BatchProcessor.inspectPng(valid,4,3);assertTrue(facts.alphaChannel&&facts.transparent&&facts.visible);assertThrows(java.io.IOException.class,()->BatchProcessor.inspectPng(valid,5,3));Path empty=directory.resolve("empty.png");ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB),"PNG",empty.toFile());assertThrows(java.io.IOException.class,()->BatchProcessor.inspectPng(empty,2,2));}
}
