package video

import drawing.convertation.Plane
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import math.Complex
import org.jcodec.api.awt.AWTSequenceEncoder
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.model.Rational
import java.awt.image.BufferedImage
import java.lang.Exception
import java.util.stream.Collectors
import kotlin.math.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


@Suppress("NAME_SHADOWING")
class VideoMaker(private val conf: VideoConfiguration) {
    enum class InterpolationMethod {
        Linear,
        CatmullRom
    }

    private var _frameHeight = 0f
    private var _frameWidth = 0f


    val aspectRatio
        get() = _frameWidth.toDouble() / _frameHeight

    fun getVideo(method:InterpolationMethod) {
        _frameWidth = conf.width
        _frameHeight = conf.height
        val images = when(method){
            InterpolationMethod.CatmullRom -> getCadresCatmullRom()
            InterpolationMethod.Linear -> getCadresLinear()
        }
        render(images)
    }

    private fun getCadresCatmullRom(): List<BufferedImage> {
        return listOf()
    }

    private fun getCadresLinear(): List<BufferedImage> {
        val cadresList: MutableList<BufferedImage> = mutableListOf()
        val framesPerSegment = (conf.duration * conf.fps) / (conf.cadres.size - 1)
        var currPlane = conf.cadres[0].plane.copy()
        for (i in 0 until conf.cadres.size - 1) {
            val stepDxMax = (conf.cadres[i + 1].plane.xMax - conf.cadres[i].plane.xMax) / framesPerSegment.toFloat()
            val stepDxMin = (conf.cadres[i + 1].plane.xMin - conf.cadres[i].plane.xMin) / framesPerSegment.toFloat()
            val stepDyMax = (conf.cadres[i + 1].plane.yMax - conf.cadres[i].plane.yMax) / framesPerSegment.toFloat()
            val stepDyMin = (conf.cadres[i + 1].plane.yMin - conf.cadres[i].plane.yMin) / framesPerSegment.toFloat()
            for (j in 0 until framesPerSegment) {
                val bi = Cadre.getImageFromPlane(currPlane, conf.width, conf.height,conf.colorScheme)
                cadresList.add(bi)
                val nxmin = currPlane.xMin + stepDxMin
                val nxmax = currPlane.xMax + stepDxMax
                val nymin = currPlane.yMin + stepDyMin
                val nymax = currPlane.yMax + stepDyMax

                currPlane = Plane(nxmin,nxmax,nymin,nymax,conf.width,conf.height).also {
                    println("${it.xMin} ${it.yMin} ${it.xMax} ${it.yMax}")
                }

            }
        }
        return cadresList
    }
    private fun render(data: List<BufferedImage>) {
        val out: SeekableByteChannel? = null
        try {
            val out = NIOUtils.writableFileChannel(conf.file);
            println(out)
            val encoder = AWTSequenceEncoder(out, Rational.R(conf.fps, 1))
            data.forEach{
                println("Идём")
                encoder.encodeImage(it)
            }
            encoder.finish();
        }
        finally {
            NIOUtils.closeQuietly(out);
        }
    }
    private fun getCenterOfShots(cadres: MutableList<Cadre>): List<Complex> =
        cadres.map {
            Complex(
                (it.plane.xMin + it.plane.xMax) * 0.5,
                (it.plane.yMin + it.plane.yMax) * 0.5
            )
    }


}

