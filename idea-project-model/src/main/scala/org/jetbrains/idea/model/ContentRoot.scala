package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class ContentRoot(base: Path,
                       sources: Seq[Path] = Seq.empty,
                       resources: Seq[Path] = Seq.empty,
                       testSources: Seq[Path] = Seq.empty,
                       testResources: Seq[Path] = Seq.empty,
                       excluded: Seq[Path] = Seq.empty)
