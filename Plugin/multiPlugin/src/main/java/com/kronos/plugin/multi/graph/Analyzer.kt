package com.kronos.plugin.multi.graph

import com.kronos.plugin.base.Log
import java.util.concurrent.ConcurrentHashMap
import org.gradle.internal.graph.DirectedGraph
import org.gradle.model.internal.core.ModelNode

/**
 * Use dfs find the circle, replace to Tarjan algorithm later.
 */
class Analyzer(private val libs: List<ModuleNode>, private val allowMiss: Boolean) {

    private val modules = ConcurrentHashMap<String, ModuleNode>()

    fun analyze(): Set<ModuleNode> {
        val walker = CachingDirectedGraphWalker(false, object : com.kronos.plugin.multi.graph.DirectedGraph<ModuleNode, ModuleNode> {
            override fun getNodeValues(node: ModuleNode, values: MutableCollection<in ModuleNode>, connectedNodes: MutableCollection<in ModuleNode>) {
                values.add(node)
                node.taskDependencies.forEach { name ->
                    modules[name]?.let {
                        getTaskModule(it, connectedNodes)
                        Log.info("connectedNodes$connectedNodes")
                    }
                }
            }
        })

        libs.forEach {
            val nodes = arrayListOf<ModuleNode>()
            modules[it.moduleName] = it
            nodes.add(it)
            synchronized(walker) {
                walker.add(nodes)
            }
        }
        return walker.findValues()
    }

    fun getTaskModule(node: ModuleNode, connectedNodes: MutableCollection<in ModuleNode>) {
        //  val list = mutableListOf<ModuleNode>()
        connectedNodes += node
        node.taskDependencies.forEach {
            modules[it]?.let { node ->
                getTaskModule(node, connectedNodes)
            }
        }
    }
}


interface Node {
    val moduleName: String
    val taskDependencies: List<String>
}
