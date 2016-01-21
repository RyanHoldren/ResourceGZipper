package com.github.ryanholdren.resourcegzipper;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractResourceGZipperMojo extends AbstractMojo {

	@Parameter(property="filter", defaultValue = "\\.(cs|j)s$")
	private String filter;

	private Log log;

	@Override
	public void setLog(Log log) {
		this.log = log;
	}

	protected abstract String getPathToResourceDirectory();

	private Path getResourceDirectory() throws MojoExecutionException {
		final String pathToDirectory = getPathToResourceDirectory();
		if (pathToDirectory == null) {
			throw new MojoExecutionException("No resource directory was not specified!");
		}
		final Path directory = Paths.get(pathToDirectory);
		if (Files.notExists(directory)) {
			throw new MojoExecutionException("The specified resource directory, '" + directory + "', does not exist!");
		}
		if (Files.isDirectory(directory)) {
			return directory;
		} else {
			throw new MojoExecutionException("The specified resource directory, '" + directory + "', is not a directory at all!");
		}
	}

	@Override
	public void execute() throws MojoExecutionException {
		final Path directory = getResourceDirectory();
		final Predicate<Path> filter = getFilter();
		try (
			final Stream<Path> resources = Files.walk(directory);
		) {
			resources.filter(filter).forEach(resource -> {
				try (
					final FileOutputStream output = new FileOutputStream(resource.toString() + ".gz")
				) {
					Files.copy(resource, output);
					log.info("GZipped resource file: '" + resource + "'.");
				} catch (Throwable exception) {
					log.error("Unexpected error GZipping resource file: '" + resource + "'.", exception);
				}
			});
		} catch (Throwable exception) {
			throw new MojoExecutionException("Could not walk the specified resource directory, '" + directory + "'!", exception);
		}
	}

	private Predicate<Path> getFilter() {
		final Pattern pattern = Pattern.compile(filter);
		return resource -> {
			final Matcher matcher = pattern.matcher(resource.toString());
			return matcher.find();
		};
	}

}