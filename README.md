# Cantaloupe

*[IIIF 2.0](http://iiif.io) image server in Java*

# Features

* Simple
* Easy to get working
* Simple
* Simple

## Supported Image Formats

Cantaloupe supports pluggable image processors, each of which may support a
different set of formats. See the Processors section below.

## Supported Image Sources

Cantaloupe supports pluggable resolvers, which can provide access to images
from different sources. Currently, the local filesystem is the only available
source.

## What It Doesn't Do

* Cache anything. It may or may not do this some day. In the meantime, a
  caching reverse proxy should be used in production.
* Write log files. Log messages go to standard output.
* Run inside an application server. ([Rationale]
  (http://johannesbrodwall.com/2010/03/08/why-and-how-to-use-jetty-in-mission-critical-production/))

# Requirements

* JRE 6+
* ImageMagick with PDF and JPEG2000 delegates (see the ImageMagickProcessor
  section below for more information)

# Configuration

Create a file called `cantaloupe.properties` anywhere on disk, and paste into
it the following contents, editing it as necessary for your site:

```
# The HTTP port to bind to.
http.port = 8182

# Set to true for debugging
print_stack_trace_on_error_page = true

# The image processor to use. The only available value is ImageMagickProcessor.
# Note that the `convert` and `identify` binaries must be in the PATH.
processor = ImageMagickProcessor

# The path resolver that translates the identifier in the URL to a path. The
# only available value is FilesystemResolver.
resolver = FilesystemResolver

# The server-side path that will be prefixed to the identifier in the URL.
FilesystemResolver.path_prefix = /home/myself/images

# The server-side path that will be suffixed to the identifier in the URL.
FilesystemResolver.path_suffix =
```

# Running

Run it like:

`$ java -jar Cantaloupe-x.x.x.jar -Dcantaloupe.config=/path/to/cantaloupe.properties`

It is now available at: `http://localhost:{http.port}/iiif/`

# Processors

Cantaloupe can use different image processors. Currently, it includes only the
ImageMagickProcessor.

## ImageMagickProcessor

ImageMagickProcessor uses [im4java](http://im4java.sourceforge.net), which
forks out to the ImageMagick `convert` and `identify` commands. These must be
in the PATH in order for them to be found.

ImageMagick is not known for being particularly fast or efficient, but it
produces high quality output. Performance degrades and memory usage
increases as image size increases.

ImageMagickProcessor supports all IIIF 2.0 formats except WebP.

# Resolvers

## FilesystemResolver

FilesystemResolver maps an identifier from an IIIF URL to a filesystem path.

### Example With Prefix

Given the following configuration options:

* `FilesystemResolver.path_prefix = /data/images`
* `FilesystemResolver.path_suffix = `

And the following URL:

* http://example.org/iiif/image.jpg/full/full/0/default.jpg

FilesystemResolver will look for an image located at
/data/images/image.jpg.


### Example With Prefix and Suffix

Given the following configuration options:

* `FilesystemResolver.path_prefix = /data/images`
* `FilesystemResolver.path_suffix = /image.jp2`

And the following URL:

* http://example.org/iiif/some-uuid/full/full/0/default.jpg

FilesystemResolver will look for an image located at
/data/images/some-uuid/image.jp2.

# Contributing

1. Fork it (https://github.com/medusa-project/cantaloupe/fork)
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request
