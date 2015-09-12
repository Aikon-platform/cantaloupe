package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.MediaType;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handles the landing page.
 */
public class LandingResource extends AbstractResource {

    private class ProcessorComparator implements Comparator<Processor> {
        public int compare(Processor o1, Processor o2) {
            return o1.getClass().getSimpleName().
                    compareTo(o2.getClass().getSimpleName());
        }
    }

    private class SourceFormatComparator implements Comparator<SourceFormat> {
        public int compare(SourceFormat o1, SourceFormat o2) {
            return o1.getPreferredExtension().
                    compareTo(o2.getPreferredExtension());
        }
    }

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("landing.vm");
        return new TemplateRepresentation(template, getTemplateVars(),
                MediaType.TEXT_HTML);
    }

    private Map<String,Object> getTemplateVars() throws Exception {
        Map<String,Object> vars = new HashMap<String,Object>();

        // resolver name
        Resolver resolver = ResolverFactory.getResolver();
        String resolverStr = "None";
        if (resolver != null) {
            resolverStr = resolver.getClass().getSimpleName();
        }
        vars.put("resolverName", resolverStr);

        // source format assignments
        Map<SourceFormat,String> assignments =
                new TreeMap<SourceFormat, String>();
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            try {
                assignments.put(sourceFormat,
                        ProcessorFactory.getProcessor(sourceFormat).getClass().getSimpleName());
            } catch (UnsupportedSourceFormatException e) {
                // noop
            }
        }
        vars.put("processorAssignments", assignments);

        // source formats
        List<SourceFormat> sourceFormats = new ArrayList<SourceFormat>();
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (sourceFormat != SourceFormat.UNKNOWN) {
                sourceFormats.add(sourceFormat);
            }
        }
        Collections.sort(sourceFormats, new SourceFormatComparator());
        vars.put("sourceFormats", sourceFormats);

        // processors
        List<Processor> sortedProcessors =
                new ArrayList<Processor>(ProcessorFactory.getAllProcessors());
        Collections.sort(sortedProcessors, new ProcessorComparator());
        vars.put("processors", sortedProcessors);

        return vars;
    }

}
