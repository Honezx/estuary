package estuary.model

import java.util.Date

import breeze.linalg.{DenseMatrix, DenseVector, max, sum}
import breeze.numerics.log
import estuary.components.initializer.{HeInitializer, WeightsInitializer}
import estuary.components.layers.Layer
import estuary.components.optimizer.{AdamOptimizer, Optimizer}
import estuary.components.regularizer.Regularizer
import estuary.model.Model.OptimizationConfig
import estuary.utils.PlotUtils
import org.apache.log4j.Logger

import scala.collection.mutable

trait Model extends Serializable{
  val logger: Logger = Logger.getLogger(Model.getClass)

  val hiddenLayers: Seq[Layer]
  val outputLayer: Layer
  val learningRate: Double
  val iterationTime: Int
  val regularizer: Option[Regularizer]

  var costHistory: mutable.MutableList[Double]
  var params: Seq[DenseMatrix[Double]]
  var labelsMapping: Vector[Int]

  lazy val allLayers: Seq[Layer] = hiddenLayers :+ outputLayer

  def init(inputDim: Int, outputDim: Int, initializer: WeightsInitializer = HeInitializer): Seq[DenseMatrix[Double]]
  def init(initParams: Seq[DenseMatrix[Double]]): Seq[DenseMatrix[Double]] = {
    params = initParams
    params
  }

  def predict(feature: DenseMatrix[Double]): DenseVector[Int]

  /**Fully functional method.*/
  def trainFunc(feature: DenseMatrix[Double], label: DenseMatrix[Double], allLayers: Seq[Layer], opConfig: OptimizationConfig, optimizer: Optimizer): Seq[DenseMatrix[Double]]

  def forward(feature: DenseMatrix[Double], params: Seq[DenseMatrix[Double]]): DenseMatrix[Double]

  def backward(label: DenseMatrix[Double], params: Seq[DenseMatrix[Double]]): Seq[DenseMatrix[Double]]

  def copyStructure: Model

  def train(feature: DenseMatrix[Double], label: DenseMatrix[Double]): this.type = {train(feature, label, AdamOptimizer())}

  def train(feature: DenseMatrix[Double], label: DenseMatrix[Double], optimizer: Optimizer): this.type = {
    init(feature.cols, label.cols)
    val opConfig = OptimizationConfig(params, learningRate, iterationTime)
    val trainedParams = trainFunc(feature, label, allLayers, opConfig, optimizer)
    params = trainedParams
    costHistory = optimizer.costHistory
    this
  }

  def train(feature: DenseMatrix[Double], label: DenseVector[Int]): this.type = {
    train(feature, label, AdamOptimizer())
  }

  def train(feature: DenseMatrix[Double], label: DenseVector[Int], optimizer: Optimizer): this.type = {
    val (labelMatrix, labelsMapping) = Model.convertVectorToMatrix(label)
    this.labelsMapping = labelsMapping.toVector
    train(feature, labelMatrix, optimizer)
  }

  def forward(feature: DenseMatrix[Double]): DenseMatrix[Double] = forward(feature, params)

  def forward(feature: DenseMatrix[Double], label: DenseMatrix[Double], params: Seq[DenseMatrix[Double]]): Double = {
    val yHat = forward(feature, params)
    Model.calCost(label, yHat, allLayers, regularizer)
  }

  def plotCostHistory(): Unit = PlotUtils.plotCostHistory(costHistory)


}


/**Util method for Neural Network Models*/
object Model {

  case class OptimizationConfig(initParams: Seq[DenseMatrix[Double]], learningRate: Double, iterationTime: Int)

  def accuracy(label: DenseVector[Int], labelPredicted: DenseVector[Int]): Double = {
    val numCorrect = (0 until label.length).map { index =>
        if (label(index) == labelPredicted(index)) 1 else 0
      }.count(_ == 1)
    numCorrect.toDouble / label.length.toDouble
  }

  def evaluationTime[T](task: => T): Long = {
    val startTime = System.currentTimeMillis()
    task
    val endTime = System.currentTimeMillis()
    endTime - startTime
  }

  def deOneHot(yHat: DenseMatrix[Double]): DenseMatrix[Int] = {
    val deOneHottedMatrix = DenseMatrix.zeros[Int](yHat.rows, yHat.cols)
    for (i <- (0 until yHat.rows).par) {
      val sliced = yHat(i, ::)
      val maxRow = max(sliced)
      deOneHottedMatrix(i, ::) := sliced.t.map(index => if (index == maxRow) 1 else 0).t
    }
    deOneHottedMatrix
  }

  def calCost(label: DenseMatrix[Double], predicted: DenseMatrix[Double], layers: Seq[Layer], regularizer: Option[Regularizer]): Double = {
    val originalCost = -sum(label *:* log(predicted + 1E-9)) / label.rows.toDouble
    val reguCost = layers.foldLeft[Double](0.0) { case (totalReguCost, layer) => totalReguCost + layer.getReguCost(regularizer)}
    val lambda = regularizer match {
      case Some(rg) => rg.lambda
      case None => 0.0
    }
    originalCost + lambda * reguCost / label.rows.toDouble
  }

  /**
    * Convert labels in a single vector to a matrix.
    * e.g. Vector(0, 1, 0, 1) => Matrix(Vector(1, 0, 1, 0), Vector(0, 1, 0, 1))
    * Vector(0, 1, 2) => Matrix(Vector(1, 0, 0), Vector(0, 1, 0), Vector(0, 0, 1))
    */
  def convertVectorToMatrix(labelVector: DenseVector[Int]): (DenseMatrix[Double], List[Int]) = {
    val labels = labelVector.toArray.toSet.toList.sorted //distinct elements by toSet.

    val numLabels = labels.size
    val res = DenseMatrix.zeros[Double](labelVector.length, numLabels)

    for ((label, i) <- labels.zipWithIndex.par) {
      val helperVector = DenseVector.ones[Int](labelVector.length) * label
      res(::, i) := elementWiseEqualCompare(labelVector, helperVector).map(_.toDouble)
    }
    (res, labels)
  }

  def convertMatrixToVector(labelMatrix: DenseMatrix[Int], labelsMapping: Vector[Int]): DenseVector[Int] = {
    val res = DenseVector.zeros[Int](labelMatrix.rows)
    for (i <- 0 until labelMatrix.cols) {
      res :+= labelMatrix(::, i) * labelsMapping(i)
    }
    res
  }

  /**
    * Compare two vector for equality in element-wise.
    * e.g. a = Vector(1, 2, 3), b = Vector(1, 0, 0), then return Vector(1, 0, 0)
    */
  def elementWiseEqualCompare(a: DenseVector[Int], b: DenseVector[Int]): DenseVector[Int] = {
    assert(a.length == b.length, "a.length != b.length")
    val compareArr = a.toArray.zip(b.toArray).par.map { case (i, j) =>
      if (i == j) 1 else 0
    }.toArray
    DenseVector(compareArr)
  }
}
