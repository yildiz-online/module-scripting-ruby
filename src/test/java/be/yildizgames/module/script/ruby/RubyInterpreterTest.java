/*
 * This file is part of the Yildiz-Engine project, licenced under the MIT License  (MIT)
 *
 * Copyright (c) 2018 Grégory Van den Borre
 *
 * More infos available: https://www.yildiz-games.be
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons
 * to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS  OR COPYRIGHT  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE  SOFTWARE.
 */

package be.yildizgames.module.script.ruby;

import be.yildizgames.common.shape.Box;
import be.yildizgames.module.script.ParsedScript;
import be.yildizgames.module.script.ScriptException;
import be.yildizgames.module.script.ScriptInterpreter;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Grégory Van den Borre
 */
class RubyInterpreterTest {


    //@Test
    void testSetOutput() throws Exception {
        ScriptInterpreter interpreter = RubyInterpreter.singleThread();
        Path folder= Files.createTempDirectory("test");
        File f = new File(folder.getRoot().toFile().getAbsolutePath() + "/test.txt");
        String toPrint = "testing output in file";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            interpreter.setOutput(bw);
            interpreter.print(toPrint);
        }
        assertTrue(f.exists());
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            assertEquals(toPrint, br.readLine());
        }
    }

    @Test
    void testRunScriptNotExists() throws Exception {
        ScriptInterpreter interpreter = RubyInterpreter.singleThread();
        assertThrows(ScriptException.class, () -> interpreter.runScript("none.rb"));

    }

    @Test
    void testRunScript() throws Exception {
        ScriptInterpreter interpreter = RubyInterpreter.singleThread();
        ParsedScript ps = interpreter.runScript(getFile().getAbsolutePath());
        ps.run();

    }

    private static File getFile() {
        return new File(RubyInterpreter.class.getClassLoader().getResource("test-unit-1.rb").getFile());
    }

    @Test
    void testRunCommand() throws Exception {
        ScriptInterpreter interpreter = RubyInterpreter.singleThread();
        assertEquals(4L, interpreter.runCommand("2+2"));
        assertEquals(null, interpreter.runCommand("puts 'testing puts return code'"));
        assertEquals(Box.cube(5), interpreter.runCommand("a = Java::be.yildizgames.common.shape.Box.new(5)"));
        assertEquals(Box.cube(5), interpreter.runCommand("a"));
    }

    @Test
    void testPrint() {
        // fail("Not yet implemented");
    }

    @Test
    void testGetFileHeader() throws Exception {
        ScriptInterpreter interpreter = RubyInterpreter.singleThread();
        assertEquals("#!//usr//bin//ruby\n", interpreter.getFileHeader());

    }

    @Test
    void testGetFileExtension() throws Exception {
        ScriptInterpreter interpreter = RubyInterpreter.singleThread();
        assertEquals("rb", interpreter.getFileExtension());
    }

    @Test
    void testConstructorThreadSafe() {
        assertNotNull(RubyInterpreter.threadSafe());
    }

    @Test
    void testConstructorSingleton() {
        assertNotNull(RubyInterpreter.singleton());
    }

    @Test
    void testConstructorConcurrent() {
        assertNotNull(RubyInterpreter.concurrent());
    }

    @Test
    void testClose() throws Exception {
        ScriptInterpreter i = RubyInterpreter.singleThread();
        assertFalse(i.isClosed());
        i.close();
        assertTrue(i.isClosed());
    }

}
