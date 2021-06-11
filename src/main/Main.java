package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.*;
import javax.imageio.stream.*;

public class Main {

    private final static String PATH = Paths.get(System.getProperty("user.home"), "MeasurementImages").toString();
    private final static float QUALITY = 0.3f;

    private final static int WIDTH = 1200;
    private final static int HEIGHT = 900;

    public static void main(String[] args) throws IOException {
        File directory;

        try {
            directory = gotoWorkingDirectory(PATH);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.out.println("Terminating because of " + ex.getMessage());
            System.exit(0);
        }

        System.out.println("Starting:");
        try (Stream<Path> paths = Files.walk(Paths.get(PATH))) {
            paths.filter(file -> Files.isRegularFile(file) && Main.isFilePNG(file))
                    .forEach(imagePath -> {
                        System.out.println("--------------------------------------------------------------");
                        String inputPngImage = getImageName(imagePath);
                        String temporaryJPG = convertFilePathToTemporaryJPG(imagePath);
                        String resizedJpgImage = convertFilePathToJPG(imagePath);
                        try {
                            System.out.println("> Trying to convert " + inputPngImage + " => " + getImageNameWithoutSuffix(inputPngImage) + ".jpg");
                            try {
                                convertToJPG(imagePath);
                            } catch (IIOException ex) {
                                ex.printStackTrace();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            System.out.println("> Trying to resize " + getImageName(Paths.get(resizedJpgImage)) + " => " + getImageNameWithoutSuffix(inputPngImage) + ".jpg");
                            resizeCompressedJPGImage(temporaryJPG, resizedJpgImage);
                            System.out.println("> Successful resizing!");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        deleteTemporaryCompressedFile(temporaryJPG);
                        System.out.println("--------------------------------------------------------------\n");
                    });
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Terminating because of " + ex.getMessage());
            System.exit(0);
        }
        System.out.println("Compression done. Closing...");
    }

    private static File gotoWorkingDirectory(String path) throws FileNotFoundException {
        File workingDirectory = new File(path);
        if (!workingDirectory.exists()) {
            throw new FileNotFoundException("Directory doesn't exist");
        } else {
            return workingDirectory;
        }
    }

    private static String getImageName(Path imagePath) {
        String imagePathInString = imagePath.toString();
        List<String> directories = new ArrayList<String>(Arrays.asList(imagePathInString.split(File.separator.equals ("\\") ? "\\\\" : "/")));

        return directories.get(4);
    }

    private static String getImageNameWithoutSuffix(String imageName) {
        String imageNameWithoutSuffix;
        if (imageName.split("\\.").length != 2) {
            return null;
        } else {
            imageNameWithoutSuffix = imageName.split("\\.")[0];
            return imageNameWithoutSuffix;
        }
    }

    private static String getImageSuffix(String imageName) {
        String suffix;
        if (imageName.split("\\.").length != 2) {
            return null;
        } else {
            suffix = imageName.split("\\.")[1];
            return suffix;
        }
    }

    private static String convertFilePathToJPG(Path imagePath) {
        return imagePath.toString().split("\\.")[0].concat(".jpg");
    }

    private static String convertFilePathToTemporaryJPG(Path imagePath) {
        return imagePath.toString().split("\\.")[0].concat("_temp.jpg");
    }

    private static boolean isFilePNG(Path imagePath) {
        String imageName = getImageName(imagePath);
        String suffix = getImageSuffix(imageName);

        if (suffix == null) {
            return false;

        }
        return suffix.equals("png");
    }

    private static void deleteTemporaryCompressedFile(String compressedJPGFile) {
       File compressedJPGFilePath = new File(compressedJPGFile);
       if (compressedJPGFilePath.delete()) {
           System.out.println("> Successfully deleted temporary image.");
       }
    }

    private static void resizeCompressedJPGImage(String compressedImagePath, String resizedAndCompressedImagePath)
            throws IOException {
        // reads input image
        File inputFile = new File(compressedImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);

        // creates output image
        BufferedImage outputImage = new BufferedImage(WIDTH,
                HEIGHT, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, WIDTH, HEIGHT, null);
        g2d.dispose();

        // extracts extension of output file
        String formatName = resizedAndCompressedImagePath.substring(resizedAndCompressedImagePath
                .lastIndexOf(".") + 1);

        // writes to output file
        ImageIO.write(outputImage, formatName, new File(resizedAndCompressedImagePath));
    }

    private static void convertToJPG(Path imagePath) throws IOException {
        // Compressing the image in JPG format
        String imageName = getImageName(imagePath);
        String compressedImageName = getImageNameWithoutSuffix(imageName) + "_temp.jpg";
        String compressedPath = Paths.get(System.getProperty("user.home"), "MeasurementImages").toString();
        compressedPath = Paths.get(compressedPath, compressedImageName).toString();

        File imageFile = new File(String.valueOf(imagePath));
        File compressedImageFile = new File(compressedPath);

        InputStream is = null;
        try {
            is = new FileInputStream(imageFile);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        OutputStream os = null;
        try {
            os = new FileOutputStream(compressedImageFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // create a BufferedImage as the result of decoding the supplied InputStream
        BufferedImage image = null;
        try {
            image = ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // get all image writers for JPG format
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");

        if (!writers.hasNext())
            throw new IllegalStateException("No writers found");

        ImageWriter writer = (ImageWriter) writers.next();
        ImageOutputStream ios = null;
        try {
            ios = ImageIO.createImageOutputStream(os);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();

        // Compress to a given quality
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(QUALITY);

        try {
            writer.write(null, new IIOImage(image, null, null), param);
            System.out.println("> Successful conversion!");
        } catch (IOException e) {
            System.out.println("** Cannot convert " + imageName + " to JPG, because of " + e.getMessage());
            System.out.println("** Deleting the JPG");
            if (compressedImageFile.delete()) {
                System.out.println("** Deleted the JPG successfully!\n");
            } else {
                System.out.println("** Cannot delete the JPG!\n");
            }
        }

        // close all streams
        try {
            is.close();
            os.close();
            assert ios != null;
            ios.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        writer.dispose();
    }
}
