package cn.jmicro.barcode;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class ZxingUtils {
	
	public static BufferedImage enQRCode(String contents, int width, int height) throws WriterException {
		// 定义二维码参数
		final Map<EncodeHintType, Object> hints = new HashMap<>(8);
		// 编码
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		// 容错级别
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
		// 边距
		hints.put(EncodeHintType.MARGIN, 0);
		return enQRCode(contents, width, height, hints);
	}

	/**
	 * 生成二维码
	 *
	 * @param contents 二维码内容
	 * @param width    图片宽度
	 * @param height   图片高度
	 * @param hints    二维码相关参数
	 * @return BufferedImage对象
	 * @throws WriterException 编码时出错
	 * @throws IOException     写入文件出错
	 */
	public static BufferedImage enQRCode(String contents, int width, int height, Map<EncodeHintType, Object> hints) throws WriterException {
//        String uuid = UUID.randomUUID().toString().replace("-", "");
		// 本地完整路径
//        String pathname = path + "/" + uuid + "." + format;
		// 生成二维码
		BitMatrix bitMatrix = new MultiFormatWriter().encode(contents, BarcodeFormat.QR_CODE, width, height, hints);
//        Path file = new File(pathname).toPath();
		// 将二维码保存到路径下
//        MatrixToImageWriter.writeToPa(bitMatrix, format, file);
//        return pathname;
		return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}

	/**
	 * 将图片绘制在背景图上
	 *
	 * @param backgroundPath 背景图路径
	 * @param zxingImage     图片
	 * @param x              图片在背景图上绘制的x轴起点
	 * @param y              图片在背景图上绘制的y轴起点
	 * @return
	 */
	public static BufferedImage drawImage(String backgroundPath, BufferedImage zxingImage, int x, int y)
			throws IOException {
		// 读取背景图的图片流
		BufferedImage backgroundImage;
		// Try-with-resources 资源自动关闭,会自动调用close()方法关闭资源,只限于实现Closeable或AutoCloseable接口的类
		URL imgurl = new URL(backgroundPath);
//        try (InputStream imagein = new FileInputStream(backgroundPath)) {
//            backgroundImage = ImageIO.read(imagein);
//        }
		try (InputStream imagein = new BufferedInputStream(imgurl.openStream())) {
			backgroundImage = ImageIO.read(imagein);
		}
		return drawImage(backgroundImage, zxingImage, x, y);
	}

	/**
	 * 将图片绘制在背景图上
	 *
	 * @param backgroundImage 背景图
	 * @param zxingImage      图片
	 * @param x               图片在背景图上绘制的x轴起点
	 * @param y               图片在背景图上绘制的y轴起点
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage drawImage(BufferedImage backgroundImage, BufferedImage zxingImage, int x, int y)
			throws IOException {
		Objects.requireNonNull(backgroundImage, ">>>>>背景图不可为空");
		Objects.requireNonNull(zxingImage, ">>>>>二维码不可为空");
		// 二维码宽度+x不可以超过背景图的宽度,长度同理
		if ((zxingImage.getWidth() + x) > backgroundImage.getWidth()
				|| (zxingImage.getHeight() + y) > backgroundImage.getHeight()) {
			throw new IOException(">>>>>二维码宽度+x不可以超过背景图的宽度,长度同理");
		}

		// 合并图片
		Graphics2D g = backgroundImage.createGraphics();
		g.drawImage(zxingImage, x, y, zxingImage.getWidth(), zxingImage.getHeight(), null);
		return backgroundImage;
	}

	/**
	 * 将文字绘制在背景图上
	 *
	 * @param backgroundImage 背景图
	 * @param x               文字在背景图上绘制的x轴起点
	 * @param y               文字在背景图上绘制的y轴起点
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage drawString(BufferedImage backgroundImage, String text, int x, int y, Font font,
			Color color) {
		// 绘制文字
		Graphics2D g = backgroundImage.createGraphics();
		// 设置颜色
		g.setColor(color);
		// 消除锯齿状
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		// 设置字体
		g.setFont(font);
		// 绘制文字
		g.drawString(text, x, y);
		return backgroundImage;
	}

	public static InputStream bufferedImageToInputStream(BufferedImage backgroundImage) throws IOException {
		return bufferedImageToInputStream(backgroundImage, "png");
	}

	/**
	 * backgroundImage 转换为输出流
	 *
	 * @param backgroundImage
	 * @param format
	 * @return
	 * @throws IOException
	 */
	public static InputStream bufferedImageToInputStream(BufferedImage backgroundImage, String format)
			throws IOException {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		try (ImageOutputStream imOut = ImageIO.createImageOutputStream(bs)) {
			ImageIO.write(backgroundImage, format, imOut);
			InputStream is = new ByteArrayInputStream(bs.toByteArray());
			return is;
		}
	}

	/**
	 * 保存为文件
	 *
	 * @param is
	 * @param fileName
	 * @throws IOException
	 */
	public static void saveFile(InputStream is, String fileName) throws IOException {
		try (BufferedInputStream in = new BufferedInputStream(is);
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName))) {
			int len;
			byte[] b = new byte[1024];
			while ((len = in.read(b)) != -1) {
				out.write(b, 0, len);
			}
		}
	}

	// 读取远程url图片,得到宽高
	public static int[] returnImgWH(URL url) {
		int[] a = new int[2];
		BufferedImage bi = null;
		boolean imgwrong = false;
		try {
			// 读取图片
			bi = javax.imageio.ImageIO.read(url);
			try {
				// 判断文件图片是否能正常显示,有些图片编码不正确
				int i = bi.getType();
				imgwrong = true;
			} catch (Exception e) {
				imgwrong = false;
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		if (imgwrong) {
			a[0] = bi.getWidth(); // 获得 宽度
			a[1] = bi.getHeight(); // 获得 高度
		} else {
			a = null;
		}
		return a;
	}

	public static void main(String[] args) {
		// 二维码宽度
		int width = 293;
		// 二维码高度
		int height = 293;
		// 二维码内容
		String contcent = "http://www.baidu.com";
		BufferedImage zxingImage = null;
		try {
			// 二维码图片流
			zxingImage = ZxingUtils.enQRCode(contcent, width, height);
		} catch (WriterException e) {
			e.printStackTrace();
		}
		// 背景图片地址
		String backgroundPath = "http://localhost:9090/_fs_/262";
		InputStream inputStream = null;
		try {
			URL url = new URL(backgroundPath);
			int[] bb = returnImgWH(url);

			// 合成二维码和背景图
			BufferedImage image = ZxingUtils.drawImage(backgroundPath, zxingImage, bb[0] - width, bb[1] - height);
//            //-----------------------------------------绘制文字-----------------------------------------------------
//            Font font = new Font("微软雅黑", Font.BOLD, 35);
//            String text = "";
//            image = ZxingUtils.drawString(image, text, 375, 647,font,new Color(244,254,189));
//            //-----------------------------------------绘制文字-----------------------------------------------------
			// 图片转inputStream
			inputStream = ZxingUtils.bufferedImageToInputStream(image);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 保存的图片路径
		String originalFileName = "D://99999.png";
		try {
			// 保存为本地图片
			ZxingUtils.saveFile(inputStream, originalFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
