package com.spoiledmilk.spritebaker;

import java.io.IOException;

@FunctionalInterface
public interface MaterialProvider530 {
    TextureMaterial530 material(int id) throws IOException;
}
