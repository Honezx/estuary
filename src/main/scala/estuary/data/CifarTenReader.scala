package estuary.data

import breeze.linalg.{DenseMatrix, DenseVector}
import estuary.model.Model
import estuary.utils.ImageReader
import org.slf4j.LoggerFactory

import scala.io.Source

/**
  * Created by mengpan on 2017/10/29.
  */
trait CifarTenReader extends Reader {
  private val log = LoggerFactory.getLogger(this.getClass)

  protected val trainingLabelPath: String
  protected val labelFilteringSeq: Seq[Int]

  override def read(filePath: String): (DenseMatrix[Double], DenseMatrix[Double]) = {
    val fileNames = getMatchedFilesName(filePath)

    val fileIndexes = fileNames.map { name =>
      val endDirIndex = name.lastIndexOf('/')
      val dotIndex = name.lastIndexOf(".")
      name.substring(endDirIndex + 1, dotIndex).toInt
    }.toVector

    val labels = getLabels(fileIndexes)

    val nameAndLabels = filterLabel(fileNames.zip(fileIndexes).toMap, labels)

    log.info("concurrently reading files...")
    val (tFeature, tLabel) = nameAndLabels.toSeq.par.map { case (name, label) =>
      val feature = ImageReader.readImageToRGBVector(name).get / 255.0
      (feature, label)
    }.seq.unzip
    log.info(s"${tFeature.length} records read")

    val label = Model.convertVectorToMatrix(new DenseVector(tLabel.seq.toArray))._1
    val rows = tFeature.length
    val cols = tFeature(0).length
    val data = DenseMatrix.zeros[Double](rows, cols)

    tFeature.seq.zipWithIndex.foreach{ case (f, i) =>
      data(i, ::) := f.t
    }

    (data, label)
  }

  private def filterLabel(nameToIndex: Map[String, Int], indexToLabel: Map[Int, Int]): Map[String, Int] = {
    nameToIndex
      .map{ case (name, index) =>
        name -> indexToLabel(index)
      }
      .filter(a => labelFilteringSeq.contains(a._2))
  }

  private def getLabels(fileIndexes: Vector[Int]): Map[Int, Int] = {
    val fileLabelMapping = Source
      .fromFile(trainingLabelPath)
      .getLines()
      .map { eachRow =>
        val split = eachRow.split(",")
        (split(0), split(1))
      }.filter { eachRow =>
      eachRow._1 != "id"
    }.map { eachRow =>
      (eachRow._1.toInt, eachRow._2)
    }.toVector

    val labels = fileLabelMapping.map(_._2).toSet.toSeq.sorted.zipWithIndex.toMap

    val intLabel = fileLabelMapping.map(a => (a._1, labels(a._2)))

    intLabel.filter(a => fileIndexes.contains(a._1)).toMap
  }
}
