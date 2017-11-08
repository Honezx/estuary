package estuary.model

import breeze.linalg.DenseMatrix
import estuary.components.initializer.WeightsInitializer
import estuary.components.layers.{ClassicLayer, ConvLayer, DropoutLayer, Layer}
import estuary.components.optimizer.{AkkaParallelOptimizer, Optimizer, ParallelOptimizer}
import estuary.components.regularizer.Regularizer

import scala.collection.mutable.ArrayBuffer

class FullyConnectedNNModel(override val hiddenLayers: Seq[Layer],
                            override val outputLayer: ClassicLayer,
                            override val regularizer: Option[Regularizer])
  extends Model[Seq[DenseMatrix[Double]]] {

  var params: Seq[DenseMatrix[Double]] = _
  var costHistory: ArrayBuffer[Double] = _
  override var labelsMapping: Vector[Int] = _

  lazy val allLayers: Seq[Layer] = hiddenLayers :+ outputLayer

  def init(inputDim: Int, outputDim: Int, initializer: WeightsInitializer): Seq[DenseMatrix[Double]] = {
    outputLayer.setPreviousHiddenUnits(hiddenLayers.last.numHiddenUnits)
    hiddenLayers.foldLeft(inputDim) {
      case (previousDim, layer: ClassicLayer) => layer.setPreviousHiddenUnits(previousDim); layer.numHiddenUnits
      case (_, layer: ConvLayer) => layer.numHiddenUnits
    }
    params = allLayers.map { layer => layer.init(initializer) }
    params
  }

  /** Fully functional method. */
  def trainFunc(feature: DenseMatrix[Double], label: DenseMatrix[Double], allLayers: Seq[Layer],
                initParams: Seq[DenseMatrix[Double]], optimizer: Optimizer): Seq[DenseMatrix[Double]] = {
    optimizer match {
      case op: ParallelOptimizer[Seq[DenseMatrix[Double]]] => op.parOptimize(feature, label, this.asInstanceOf[Model[Seq[DenseMatrix[Double]]]], initParams)
      case op: AkkaParallelOptimizer[Seq[DenseMatrix[Double]]] => op.parOptimize(this)
      case _ => optimizer.optimize(feature, label)(initParams)(forward)(backward)
    }
  }

  def multiNodesParTrain(op: AkkaParallelOptimizer[Seq[DenseMatrix[Double]]]): this.type = {
    val trainedParams = op.parOptimize(this)
    this.params = trainedParams
    this.costHistory = op.costHistory
    this
  }

  def predict(feature: DenseMatrix[Double]): DenseMatrix[Int] = {
    val filtered = allLayers.zip(params).filter(!_._1.isInstanceOf[DropoutLayer]).unzip
    val yHat = forward(feature, filtered._2, filtered._1)
    Model.deOneHot(yHat)
  }

  def forward(feature: DenseMatrix[Double], params: Seq[DenseMatrix[Double]], allLayers: Seq[Layer]): DenseMatrix[Double] = {
    allLayers.zip(params).par.foreach { case (layer, param) => layer.setParam(param) }
    allLayers.foldLeft(feature) { (yPrevious, layer) => layer.forward(yPrevious) }
  }

  def backward(label: DenseMatrix[Double], params: Seq[DenseMatrix[Double]]): Seq[DenseMatrix[Double]] = {
    allLayers.zip(params).par.foreach { case (layer, param) => layer.setParam(param) }
    allLayers.scanRight((label, DenseMatrix.zeros[Double](1, 1))) { case (layer, (dYCurrent, _)) =>
      layer.backward(dYCurrent, regularizer)
    }.init.map(_._2).toList
  }

  def copyStructure: FullyConnectedNNModel = {
    val newModel = new FullyConnectedNNModel(hiddenLayers.map(_.copyStructure), outputLayer.copyStructure.asInstanceOf[ClassicLayer], regularizer)
    newModel
  }
}
