import breeze.stats.distributions.Rand
import org.graphstream.graph.{Edge, Node}
import org.graphstream.graph.implementations.MultiGraph

import scala.collection.JavaConversions._

class WebsiteStateVisualization(val website: Website) extends WebsiteState {
  // System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer")

  protected lazy val graph = {
    val graph = new MultiGraph("Website", false, true)
    graph.addAttribute("ui.quality")
    graph.addAttribute("ui.antialias")
    graph.addAttribute(
        "ui.stylesheet",
        "node {fill-color: red; size-mode: dyn-size;} edge {fill-color:grey;}")

    website.pages.foreach(
        page => {
      val node = graph.addNode[Node](page.id)
      node.setAttribute("ui.label", page.id)
      node.setAttribute("ui.size", Double.box(1))
    })

    website.pages.foreach(
        page => {
      page.links.foreach(link => {
        graph.addEdge[Edge](
            s"${page.id}-${link.id}-${Rand.randInt(1000)}", page.id, link.id)
      })
    })

    graph
  }

  override def display() = graph.display()

  override def visitPage(user: User, page: Page) = {
    super.visitPage(user, page)

    val node = graph.getNode[Node](page.id)
    val currSize = node.getAttribute[Double]("ui.size")
    node.setAttribute("ui.size", Double.box(currSize + 0.05))
    normalizeSizes()

    def normalizeSizes() {
      graph
        .getNodeIterator[Node]
        .foreach(node => {
          val currSize = node.getAttribute[Double]("ui.size")
          if (currSize > 10)
            node.setAttribute("ui.size", Double.box(currSize / 10.0))
        })
    }
  }
}
