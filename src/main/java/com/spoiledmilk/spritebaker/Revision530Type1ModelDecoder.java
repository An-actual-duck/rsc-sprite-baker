package com.spoiledmilk.spritebaker;

import java.util.ArrayList;
import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;

/** Exact, bounded port of revision-530 RawModel.decodeNew. */
final class Revision530Type1ModelDecoder {
    private static final int FOOTER_SIZE = 23;

    boolean matches(byte[] data) {
        try {
            return Layout.parse(data).matches;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    ModelDefinition decode(int modelId, byte[] data) {
        Layout l = Layout.parse(data);
        if (!l.matches) {
            throw new IllegalArgumentException("model " + modelId
                + " is not an exact revision-530 type-1 layout");
        }

        ModelDefinition530 out = new ModelDefinition530();
        out.id = modelId;
        out.vertexCount = l.vertices;
        out.faceCount = l.faces;
        out.numTextureFaces = l.textures;
        out.vertexX = new int[l.vertices];
        out.vertexY = new int[l.vertices];
        out.vertexZ = new int[l.vertices];
        out.faceIndices1 = new int[l.faces];
        out.faceIndices2 = new int[l.faces];
        out.faceIndices3 = new int[l.faces];
        out.faceColors = new short[l.faces];
        if (l.vertexBones == 1) out.packedVertexGroups = new int[l.vertices];
        if (l.triangleInfo) out.faceRenderTypes = new byte[l.faces];
        if (l.priority == 255) out.faceRenderPriorities = new byte[l.faces];
        else out.priority = (byte) l.priority;
        if (l.alpha == 1) out.faceTransparencies = new byte[l.faces];
        if (l.faceBones == 1) out.packedTransparencyVertexGroups = new int[l.faces];
        if (l.faceTextures == 1) out.faceTextures = new short[l.faces];
        if (l.faceTextures == 1 && l.textures > 0) out.textureCoords = new byte[l.faces];
        if (l.textures > 0) {
            out.textureRenderTypes = l.textureTypes.clone();
            out.texIndices1 = new short[l.textures];
            out.texIndices2 = new short[l.textures];
            out.texIndices3 = new short[l.textures];
        }
        if (l.complex > 0) {
            out.textureScaleX = new short[l.textures];
            out.textureScaleY = new short[l.textures];
            out.textureScaleZ = new short[l.textures];
            out.textureRotation = new byte[l.textures];
            out.textureDirection = new byte[l.textures];
            out.textureTranslation = new byte[l.textures];
        }
        if (l.cube > 0) {
            out.textureCubeU = new byte[l.textures];
            out.textureCubeV = new byte[l.textures];
        }

        Cursor vertexFlags = l.cursor(data, "vertex-flags");
        Cursor dx = l.cursor(data, "vertex-x");
        Cursor dy = l.cursor(data, "vertex-y");
        Cursor dz = l.cursor(data, "vertex-z");
        Cursor vertexGroups = l.cursor(data, "vertex-bones");
        int x = 0, y = 0, z = 0;
        for (int vertex = 0; vertex < l.vertices; vertex++) {
            int flags = vertexFlags.u8();
            if ((flags & 1) != 0) x += dx.signedShortSmart();
            if ((flags & 2) != 0) y += dy.signedShortSmart();
            if ((flags & 4) != 0) z += dz.signedShortSmart();
            out.vertexX[vertex] = x;
            out.vertexY[vertex] = y;
            out.vertexZ[vertex] = z;
            if (out.packedVertexGroups != null) out.packedVertexGroups[vertex] = vertexGroups.u8();
        }
        vertexFlags.requireEnd(); dx.requireEnd(); dy.requireEnd(); dz.requireEnd(); vertexGroups.requireEnd();

        Cursor colors = l.cursor(data, "face-colors");
        Cursor info = l.cursor(data, "triangle-info");
        Cursor priorities = l.cursor(data, "face-priorities");
        Cursor alphas = l.cursor(data, "face-alpha");
        Cursor faceGroups = l.cursor(data, "face-bones");
        Cursor textures = l.cursor(data, "face-textures");
        Cursor textureCoords = l.cursor(data, "texture-coordinates");
        for (int face = 0; face < l.faces; face++) {
            out.faceColors[face] = (short) colors.u16();
            if (out.faceRenderTypes != null) out.faceRenderTypes[face] = info.i8();
            if (out.faceRenderPriorities != null) out.faceRenderPriorities[face] = priorities.i8();
            if (out.faceTransparencies != null) out.faceTransparencies[face] = alphas.i8();
            if (out.packedTransparencyVertexGroups != null) out.packedTransparencyVertexGroups[face] = faceGroups.u8();
            if (out.faceTextures != null) out.faceTextures[face] = (short) (textures.u16() - 1);
            if (out.textureCoords != null) {
                out.textureCoords[face] = out.faceTextures[face] == -1
                    ? (byte) -1 : (byte) (textureCoords.u8() - 1);
                if (out.textureCoords[face] != -1
                    && Byte.toUnsignedInt(out.textureCoords[face]) >= l.textures) {
                    throw textureCoords.failure("mapping " + Byte.toUnsignedInt(out.textureCoords[face])
                        + " is outside " + l.textures + " texture faces");
                }
            }
        }
        colors.requireEnd(); info.requireEnd(); priorities.requireEnd(); alphas.requireEnd();
        faceGroups.requireEnd(); textures.requireEnd(); textureCoords.requireEnd();

        Cursor indexTypes = l.cursor(data, "face-index-types");
        Cursor indices = l.cursor(data, "face-indices");
        int a = 0, b = 0, c = 0, last = 0;
        for (int face = 0; face < l.faces; face++) {
            int type = indexTypes.u8();
            if (type == 1) {
                a = indices.signedShortSmart() + last;
                b = indices.signedShortSmart() + a;
                c = indices.signedShortSmart() + b;
                last = c;
            } else if (type == 2) {
                b = c; c = indices.signedShortSmart() + last; last = c;
            } else if (type == 3) {
                a = c; c = indices.signedShortSmart() + last; last = c;
            } else if (type == 4) {
                int priorA = a; a = b; b = priorA;
                c = indices.signedShortSmart() + last; last = c;
            } else {
                throw indexTypes.failure("unsupported delta-code " + type);
            }
            requireVertex(modelId, face, "A", a, l.vertices);
            requireVertex(modelId, face, "B", b, l.vertices);
            requireVertex(modelId, face, "C", c, l.vertices);
            out.faceIndices1[face] = a;
            out.faceIndices2[face] = b;
            out.faceIndices3[face] = c;
        }
        indexTypes.requireEnd(); indices.requireEnd();

        Cursor simplePmn = l.cursor(data, "simple-texture-pmn");
        Cursor complexPmn = l.cursor(data, "complex-texture-pmn");
        Cursor scales = l.cursor(data, "complex-texture-scale");
        Cursor rotations = l.cursor(data, "complex-texture-rotation");
        Cursor directions = l.cursor(data, "complex-texture-direction");
        Cursor auxiliary = l.cursor(data, "complex-texture-auxiliary");
        for (int texture = 0; texture < l.textures; texture++) {
            int type = Byte.toUnsignedInt(out.textureRenderTypes[texture]);
            Cursor pmn = type == 0 ? simplePmn : complexPmn;
            out.texIndices1[texture] = (short) pmn.u16();
            out.texIndices2[texture] = (short) pmn.u16();
            out.texIndices3[texture] = (short) pmn.u16();
            if (type != 0) {
                out.textureScaleX[texture] = (short) scales.u16();
                out.textureScaleY[texture] = (short) scales.u16();
                out.textureScaleZ[texture] = (short) scales.u16();
                out.textureRotation[texture] = rotations.i8();
                out.textureDirection[texture] = directions.i8();
                out.textureTranslation[texture] = auxiliary.i8();
                if (type == 2) {
                    out.textureCubeU[texture] = auxiliary.i8();
                    out.textureCubeV[texture] = auxiliary.i8();
                }
            } else {
                requireTextureVertex(modelId, texture, "P", out.texIndices1[texture], l.vertices);
                requireTextureVertex(modelId, texture, "M", out.texIndices2[texture], l.vertices);
                requireTextureVertex(modelId, texture, "N", out.texIndices3[texture], l.vertices);
            }
        }
        simplePmn.requireEnd(); complexPmn.requireEnd(); scales.requireEnd(); rotations.requireEnd();
        directions.requireEnd(); auxiliary.requireEnd();

        out.computeNormals();
        if (l.complex == 0) out.computeTextureUVCoordinates();
        out.computeAnimationTables();
        return out;
    }

    private static void requireVertex(int modelId, int face, String slot, int vertex, int count) {
        if (vertex < 0 || vertex >= count) throw new IllegalArgumentException("model " + modelId
            + " face " + face + " index " + slot + " is " + vertex + " for " + count + " vertices");
    }

    private static void requireTextureVertex(int modelId,int texture,String slot,short encoded,int count){
        int vertex=Short.toUnsignedInt(encoded);
        if(vertex>=count)throw new IllegalArgumentException("model "+modelId+" texture face "+texture
            +" index "+slot+" is "+vertex+" for "+count+" vertices");
    }

    static final class Layout {
        final boolean matches;
        final int footer, vertices, faces, textures, priority, alpha, faceBones, faceTextures, vertexBones;
        final boolean triangleInfo;
        final byte[] textureTypes;
        final int simple, complex, cube;
        final List<Section> sections;

        private Layout(boolean matches, int footer, int vertices, int faces, int textures,
                       int info, int priority, int alpha, int faceBones, int faceTextures,
                       int vertexBones, byte[] textureTypes, int simple, int complex, int cube,
                       List<Section> sections) {
            this.matches = matches; this.footer = footer; this.vertices = vertices; this.faces = faces;
            this.textures = textures; this.triangleInfo = (info & 1) != 0; this.priority = priority;
            this.alpha = alpha; this.faceBones = faceBones; this.faceTextures = faceTextures;
            this.vertexBones = vertexBones; this.textureTypes = textureTypes;
            this.simple = simple; this.complex = complex; this.cube = cube; this.sections = sections;
        }

        static Layout parse(byte[] data) {
            if (data == null || data.length < FOOTER_SIZE) throw new IllegalArgumentException("type-1 footer: requested " + FOOTER_SIZE + " bytes, remaining " + (data == null ? 0 : data.length));
            int footer = data.length - FOOTER_SIZE;
            if (u8(data, data.length - 2) != 255 || u8(data, data.length - 1) != 255) {
                return new Layout(false, footer, 0, 0, 0, 0, 0, 0, 0, 0, 0, new byte[0], 0, 0, 0, List.of());
            }
            int p = footer;
            int vertices = u16(data, p); p += 2;
            int faces = u16(data, p); p += 2;
            int textures = u8(data, p++);
            int info = u8(data, p++), priority = u8(data, p++), alpha = u8(data, p++);
            int faceBones = u8(data, p++), faceTextures = u8(data, p++), vertexBones = u8(data, p++);
            int dx = u16(data, p); p += 2;
            int dy = u16(data, p); p += 2;
            int dz = u16(data, p); p += 2;
            int faceIndices = u16(data, p); p += 2;
            int textureCoordinates = u16(data, p);
            if (textures > footer) return new Layout(false, footer, vertices, faces, textures, info,
                priority, alpha, faceBones, faceTextures, vertexBones, new byte[0], 0, 0, 0, List.of());
            byte[] textureTypes = new byte[textures];
            int simple = 0, complex = 0, cube = 0;
            for (int i = 0; i < textures; i++) {
                int type = u8(data, i);
                if (type > 3) return new Layout(false, footer, vertices, faces, textures, info,
                    priority, alpha, faceBones, faceTextures, vertexBones, textureTypes, simple, complex, cube, List.of());
                textureTypes[i] = (byte) type;
                if (type == 0) simple++; else complex++;
                if (type == 2) cube++;
            }
            List<Section> sections = new ArrayList<>();
            long offset = textures;
            offset = add(sections, "vertex-flags", offset, vertices, footer);
            offset = add(sections, "triangle-info", offset, (info & 1) != 0 ? faces : 0, footer);
            offset = add(sections, "face-index-types", offset, faces, footer);
            offset = add(sections, "face-priorities", offset, priority == 255 ? faces : 0, footer);
            offset = add(sections, "face-bones", offset, faceBones == 1 ? faces : 0, footer);
            offset = add(sections, "vertex-bones", offset, vertexBones == 1 ? vertices : 0, footer);
            offset = add(sections, "face-alpha", offset, alpha == 1 ? faces : 0, footer);
            offset = add(sections, "face-indices", offset, faceIndices, footer);
            offset = add(sections, "face-textures", offset, faceTextures == 1 ? (long) faces * 2 : 0, footer);
            offset = add(sections, "texture-coordinates", offset, textureCoordinates, footer);
            offset = add(sections, "face-colors", offset, (long) faces * 2, footer);
            offset = add(sections, "vertex-x", offset, dx, footer);
            offset = add(sections, "vertex-y", offset, dy, footer);
            offset = add(sections, "vertex-z", offset, dz, footer);
            offset = add(sections, "simple-texture-pmn", offset, (long) simple * 6, footer);
            offset = add(sections, "complex-texture-pmn", offset, (long) complex * 6, footer);
            offset = add(sections, "complex-texture-scale", offset, (long) complex * 6, footer);
            offset = add(sections, "complex-texture-rotation", offset, complex, footer);
            offset = add(sections, "complex-texture-direction", offset, complex, footer);
            offset = add(sections, "complex-texture-auxiliary", offset, (long) complex + (long) cube * 2, footer);
            boolean matches = (info & 2) == 0 && offset == footer;
            return new Layout(matches, footer, vertices, faces, textures, info, priority, alpha,
                faceBones, faceTextures, vertexBones, textureTypes, simple, complex, cube, sections);
        }

        Cursor cursor(byte[] data, String name) {
            for (Section section : sections) if (section.name.equals(name)) return new Cursor(data, section);
            throw new AssertionError(name);
        }

        List<String> sectionNames() {
            List<String> names=new ArrayList<>(sections.size());
            for(Section section:sections)names.add(section.name);
            return names;
        }

        boolean boundariesAreContiguous() {
            int expected=textures;
            for(Section section:sections){if(section.start!=expected||section.end<section.start||section.end>footer)return false;expected=section.end;}
            return expected==footer;
        }

        private static long add(List<Section> sections, String name, long start, long length, int footer) {
            long end = start + length;
            if (start < 0 || length < 0 || end < start || end > footer) {
                throw new IllegalArgumentException(name + " boundary: offset " + start + ", requested "
                    + length + " bytes, remaining " + Math.max(0L, (long) footer - start));
            }
            sections.add(new Section(name, (int) start, (int) end));
            return end;
        }
    }

    private static final class Section {
        final String name; final int start, end;
        Section(String name, int start, int end) { this.name = name; this.start = start; this.end = end; }
    }

    private static final class Cursor {
        final byte[] data; final Section section; int offset;
        Cursor(byte[] data, Section section) { this.data = data; this.section = section; this.offset = section.start; }
        int u8() { require(1); return Revision530Type1ModelDecoder.u8(data, offset++); }
        byte i8() { return (byte) u8(); }
        int u16() { require(2); int value = Revision530Type1ModelDecoder.u16(data, offset); offset += 2; return value; }
        int signedShortSmart() { require(1); return Revision530Type1ModelDecoder.u8(data, offset) < 128 ? u8() - 64 : u16() - 49152; }
        void requireEnd() { if (offset != section.end) throw failure("has " + (section.end - offset) + " unread bytes"); }
        IllegalArgumentException failure(String detail) { return new IllegalArgumentException(section.name + " at offset " + offset + ": " + detail); }
        void require(int count) { if (offset < section.start || count < 0 || offset + count > section.end)
            throw new IllegalArgumentException(section.name + " at offset " + offset + ": requested " + count
                + " bytes, remaining " + Math.max(0, section.end - offset)); }
    }

    private static int u8(byte[] data, int offset) { return data[offset] & 255; }
    private static int u16(byte[] data, int offset) { return u8(data, offset) << 8 | u8(data, offset + 1); }
}
