package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.ext.AngularJSClientExtension;
import cz.habarta.typescript.generator.ext.EnumConstantsExtension;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SpringApplicationTest {

    private static File sherbookOutFile() {
        File output = null;
        if (!Files.exists(Paths.get("./build/index.ts"))) {
            try {
                java.nio.file.Path sherbookDir = Paths.get("./build");
                if (!Files.exists(sherbookDir)) {
                    sherbookDir = Files.createDirectory(Paths.get("./build"));
                }
                return Files.createFile(sherbookDir.resolve("index.ts")).toFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return Paths.get("./build/index.ts").toFile();
        }
    }

    @Test
    public void testSpring() throws IOException {
        final Settings settings = TestUtils.settings();

        // output settings
        settings.outputKind = TypeScriptOutputKind.module;
        settings.outputFileType = TypeScriptFileType.implementationFile;

        // extensions
        settings.extensions = new ArrayList<>();
        settings.extensions.add(new EnumConstantsExtension()); // enum helpers
        settings.extensions.add(new AngularJSClientExtension());

        // spring will try first
        settings.generateJaxrsApplicationInterface = true;
        settings.generateJaxrsApplicationClient = true;

        String output = new TypeScriptGenerator(settings).generateTypeScript(
                Input.from(SpringApplicationTest.OrganizationController.class)
        );

        Assert.assertTrue("Wrong output: " + output, output.contains("RestApplicationClient<O> implements RestApplication<O>"));
        Assert.assertTrue("Wrong output: " + output, output.contains("export interface Person"));
        Assert.assertTrue("Wrong output: " + output, output.contains("ng.IPromise<R>"));
    }

    public enum Sex {
        MALE("MALE"),
        FEMALE("FEMALE");

        private String sex;

        Sex(String sex) {
            this.sex = sex;
        }
    }

    @Controller
    @RequestMapping("/organization")
    public static class OrganizationController {

        /*
         * This method has no request body
         */
        @RequestMapping(value = "/employees", method = RequestMethod.GET)
        @ResponseBody
        public List<Person> getEmployees() {
            List<Person> people = new ArrayList<>();

            Person jake = new Person();
            jake.name = "Jake";
            jake.sex = Sex.MALE;
            jake.age = 25;
            jake.tags = Arrays.asList("jake@example.com");
            people.add(jake);

            Person jade = new Person();
            jade.name = "Jade";
            jade.sex = Sex.FEMALE;
            jade.age = 23;
            jade.tags = Arrays.asList("jade@example.com");
            people.add(jade);

            return people;
        }

        /*
         * Has both request and response bodies
         */
        @RequestMapping(value = "/employeeByName", method = RequestMethod.POST)
        @ResponseBody
        public Person getEmployeeByName(@RequestBody EmployeeRequest request) {
            if (request.getNamePattern() != null) {
                Person jake = makeJake();
                jake.name = request.getNamePattern();
                return jake;
            }
            return null;
        }

        /*
         * Annotation on response parameter
         */
        @RequestMapping(value = "/jake", method = RequestMethod.POST)
        public
        @ResponseBody
        Person getJake() {
            return makeJake();
        }

        /*
         * Dumb endpoint that just logs its accesses
         */
        @RequestMapping(value = "/access")
        @ResponseStatus(value = HttpStatus.I_AM_A_TEAPOT)
        public void dumbMethod() {
            System.out.println("Access happened");
        }

        /*
         * Public utility method.
         * Should not be present
         */
        public Person makeJake() {
            Person jake = new Person();
            jake.name = "Jake";
            jake.age = 25;
            jake.tags = Arrays.asList("jake@example.com");

            return jake;
        }

        /*
         * Private utility method.
         * Should not be present
         */
        private Person makeJade() {
            Person jade = new Person();
            jade.name = "Jade";
            jade.age = 23;
            jade.tags = Arrays.asList("jade@example.com");

            return jade;
        }
    }

}
