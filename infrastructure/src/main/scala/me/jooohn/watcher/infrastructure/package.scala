package me.jooohn.watcher

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import cats.implicits._
import javax.imageio.ImageIO

import scala.util.Try

package object infrastructure {

  implicit class ByteArrayOps(byteArray: Array[Byte]) {

    def toBufferedImage: Try[BufferedImage] =
      Try {
        val is = new ByteArrayInputStream(byteArray)
        try {
          ImageIO.read(is)
        } finally {
          is.close()
        }
      }

  }

  implicit class BufferedImageOps(image: BufferedImage) {

    def toByteArray: Try[Array[Byte]] =
      Try {
        val os = new ByteArrayOutputStream()
        try {
          ImageIO.write(image, "jpg", os)
          os.flush()
          os.toByteArray
        } finally {
          os.close()
        }
      }

    def similarityTo(that: BufferedImage): Try[BigDecimal] = Try {
      val width = image.getWidth
      val height = image.getHeight
      if (width == that.getWidth && height == that.getHeight) {
        val thisRGBs = image.getRGB(0, 0, width, height, null, 0, width)
        val thatRGBs = that.getRGB(0, 0, width, height, null, 0, width)
        val sumOfDiffs = thisRGBs.zip(thatRGBs).toList.foldMap(Function.tupled(diffOfPixels)).toDouble
        val maxDiffs = 3L * 255 * width * height
        BigDecimal(1L) - (BigDecimal(sumOfDiffs) / BigDecimal(maxDiffs))
      } else BigDecimal.exact(0L)
    }

    private[this] def diffOfPixels(rgb1: Int, rgb2: Int): Int = {
      val r1 = (rgb1 >> 16) & 0xff
      val g1 = (rgb1 >>  8) & 0xff
      val b1 =  rgb1        & 0xff
      val r2 = (rgb2 >> 16) & 0xff
      val g2 = (rgb2 >>  8) & 0xff
      val b2 =  rgb2        & 0xff
      Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2)
    }

  }

}
