/**
 * 
 */
package org.apache.maven.plugin.fatjar;

/**
 * @author �  <ychao@bankcomm.com>
 *
 * 2010-1-23
 */
import java.io.File;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Base class for creating a jar from project classes.
 */
public abstract class AbstractJarMojo extends AbstractMojo {

	private static final String[] DEFAULT_EXCLUDES = new String[] { "**/package.html" };

	private static final String[] DEFAULT_INCLUDES = new String[] { "**/**" };

	/**
	 * List of files to include. Specified as fileset patterns.
	 */
	@Parameter
	protected String[] includes;

	/**
	 * List of files to exclude. Specified as fileset patterns.
	 */
	@Parameter
	protected String[] excludes;

	/**
	 * Directory containing the generated JAR.
	 */
	@Parameter(defaultValue = "${project.build.directory}", required = true)
	protected File outputDirectory;

	/**
	 * Name of the generated JAR.
	 */
	@Parameter(defaultValue="${project.build.finalName}", alias="jarName", required=true, property = "jar.finalName")
	protected String finalName;

	/**
	 * The Jar archiver.
	 * 
	 */
	@Component(role = org.codehaus.plexus.archiver.Archiver.class, hint = "jar")
	protected JarArchiver jarArchiver;

	/**
	 * The Maven session.
	 */
	@Parameter(defaultValue = "${session}", readonly = true)
	protected MavenSession session;

	/**
	 * The Maven project.
	 */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;

	/**
	 * The archive configuration to use.
	 * 
	 * See <a
	 * href="http://maven.apache.org/shared/maven-archiver/index.html">the
	 * documentation for Maven Archiver</a>.
	 */
	@Parameter
	protected MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

	/**
	 * Path to the default MANIFEST file to use. It will be used if
	 * <code>useDefaultManifestFile</code> is set to <code>true</code>.
	 * 
	 * @since 2.2
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF", required = true, readonly = true)
	protected File defaultManifestFile;

	/**
	 * Set this to <code>true</code> to enable the use of the
	 * <code>defaultManifestFile</code>.
	 * 
	 * @since 2.2
	 */
	@Parameter(defaultValue = "false", property = "jar.useDefaultManifestFile")
	protected boolean useDefaultManifestFile;

	/**
	 * 
	 */
	@Parameter(defaultValue = "", property = "jar.classpathPrefix")
	protected String classpathPrefix;

	/**
	 */
	@Component
	protected MavenProjectHelper projectHelper;

	/**
	 * Whether creating the archive should be forced.
	 */
	@Parameter(property = "jar.forceCreation", defaultValue = "false")
	protected boolean forceCreation;

	/**
	 * Return the specific output directory to serve as the root for the
	 * archive.
	 */
	protected abstract File getClassesDirectory();

	protected final MavenProject getProject() {
		return project;
	}

	/**
	 * Overload this to produce a jar with another classifier, for example a
	 * test-jar.
	 */
	protected abstract String getClassifier();

	/**
	 * Overload this to produce a test-jar, for example.
	 */
	protected abstract String getType();

	protected static File getJarFile(File basedir, String finalName,
			String classifier) {
		if (classifier == null) {
			classifier = "";
		} else if (classifier.trim().length() > 0
				&& !classifier.startsWith("-")) {
			classifier = "-" + classifier;
		}

		return new File(basedir, finalName + classifier + ".jar");
	}

	/**
	 * Default Manifest location. Can point to a non existing file. Cannot
	 * return null.
	 */
	protected File getDefaultManifestFile() {
		return defaultManifestFile;
	}

	/**
	 * Generates the JAR.
	 * 
	 * @todo Add license files in META-INF directory.
	 */
	public File createArchive() throws MojoExecutionException {
		File jarFile = getJarFile(outputDirectory, finalName, getClassifier());

		MavenArchiver archiver = new MavenArchiver();

		archiver.setArchiver(jarArchiver);

		archiver.setOutputFile(jarFile);

		// archive.setForced( forceCreation );

		try {
			File contentDirectory = getClassesDirectory();
			if (!contentDirectory.exists()) {
				getLog()
						.warn(
								"JAR will be empty - no content was marked for inclusion!");
			} else {
				archiver.getArchiver().addDirectory(contentDirectory,
						getIncludes(), getExcludes());
			}

			File existingManifest = getDefaultManifestFile();

			if (useDefaultManifestFile && existingManifest.exists()
					&& archive.getManifestFile() == null) {
				getLog().info(
						"Adding existing MANIFEST to archive. Found under: "
								+ existingManifest.getPath());
				archive.setManifestFile(existingManifest);
			}
			archive.getManifest().setClasspathPrefix(classpathPrefix);
			archiver.createArchive(session, project, archive);

			return jarFile;
		} catch (Exception e) {
			// TODO: improve error handling
			throw new MojoExecutionException("Error assembling JAR", e);
		}
	}

	/**
	 * Generates the JAR.
	 * 
	 * @todo Add license files in META-INF directory.
	 */
	public void execute() throws MojoExecutionException {
		File jarFile = createArchive();

		String classifier = getClassifier();
		if (classifier != null) {
			projectHelper.attachArtifact(getProject(), getType(), classifier,
					jarFile);
		} else {
			getProject().getArtifact().setFile(jarFile);
		}
	}

	private String[] getIncludes() {
		if (includes != null && includes.length > 0) {
			return includes;
		}
		return DEFAULT_INCLUDES;
	}

	private String[] getExcludes() {
		if (excludes != null && excludes.length > 0) {
			return excludes;
		}
		return DEFAULT_EXCLUDES;
	}
}
