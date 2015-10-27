package org.jetbrains.idea.model

/**
 * @author Pavel Fatin
 */
case class AndroidFacet(targetVersion: String,
                        manifest: Path,
                        apk: Path,
                        resources: Path,
                        assets: Path,
                        generator: Path,
                        libraries: Path,
                        isLibrary: Boolean,
                        proguardConfig: Seq[String])