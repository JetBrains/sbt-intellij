package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class SbtData(imports: Seq[String] = Seq.empty,
                   resolvers: Seq[Resolver] = Seq.empty)