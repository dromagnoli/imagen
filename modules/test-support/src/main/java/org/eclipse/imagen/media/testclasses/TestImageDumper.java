/* Copyright (c) 2025 Daniele Romagnoli and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 */
package org.eclipse.imagen.media.testclasses;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import org.eclipse.imagen.PlanarImage;

/**
 * Utility class to dump images during tests, mainly for debugging/comparison purposes.
 *
 * <p>Currently supports saving images as Deflate-compressed TIFF files.
 */
public class TestImageDumper {

    private TestImageDumper() {}

    public static void saveAsDeflateTiff(Path path, RenderedImage image) {

        try {

            // Pick a TIFF writer
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
            if (!writers.hasNext()) {
                throw new IllegalStateException(
                        "No TIFF ImageWriter found. Add a TIFF plugin (e.g., jai-imageio or TwelveMonkeys).");
            }
            ImageWriter writer = writers.next();

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(path.toFile())) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String deflate = findCompressionTypeIgnoreCase(param, "Deflate", "ZLib", "ZIP");
                if (deflate != null) {
                    param.setCompressionType(deflate);
                } else {
                    param.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
                }

                IIOMetadata metadata =
                        writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), param);

                writer.write(null, new IIOImage(image, null, metadata), param);
            } finally {
                writer.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image: " + e.getMessage(), e);
        }
        if (image instanceof PlanarImage) {
            ((PlanarImage) image).dispose();
        }
    }

    private static String findCompressionTypeIgnoreCase(ImageWriteParam p, String... wanted) {
        String[] types = p.getCompressionTypes();
        if (types == null) return null;
        for (String w : wanted) {
            for (String t : types) {
                if (t.equalsIgnoreCase(w)) return t;
            }
        }
        return null;
    }
}
