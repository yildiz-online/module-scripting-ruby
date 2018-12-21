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

import be.yildizgames.module.script.ParsedScript;
import be.yildizgames.module.script.ScriptException;
import be.yildizgames.module.script.ScriptInterpreter;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ParseFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;

/**
 * Ruby implementation as script interpreter.
 *
 * @author Grégory Van den Borre
 */
public final class RubyInterpreter extends ScriptInterpreter {

    private final Logger logger = LoggerFactory.getLogger(RubyInterpreter.class);

    /**
     * JRuby container to execute scripts.
     */
    private final ScriptingContainer container;

    /**
     * Flag to check if the container is closed.
     */
    private boolean closed;

    /**
     * Simple constructor.
     * @param s Scope.
     */
    private RubyInterpreter(LocalContextScope s) {
        super();
        this.container = new ScriptingContainer(s, LocalVariableBehavior.PERSISTENT);
    }

    /**
     * +------------------+ +------------------+ +------------------+
     * |   Variable Map   | |   Variable Map   | |   Variable Map   |
     * +------------------+ +------------------+ +------------------+
     * +------------------+ +------------------+ +------------------+
     * |   Ruby runtime   | |   Ruby runtime   | |   Ruby runtime   |
     * +------------------+ +------------------+ +------------------+
     * +------------------+ +------------------+ +------------------+
     * |ScriptingContainer| |ScriptingContainer| |ScriptingContainer|
     * +------------------+ +------------------+ +------------------+
     * +------------------------------------------------------------+
     * |                         JVM                                |
     * +------------------------------------------------------------+
     * @return A new RubyInterpreter with simple single thread scope.
     */
    public static RubyInterpreter singleThread() {
        return new RubyInterpreter(LocalContextScope.SINGLETHREAD);
    }

    /**
     * +------------------+ +------------------+ +------------------+
     * |   Variable Map   | |   Variable Map   | |   Variable Map   |
     * +------------------+ +------------------+ +------------------+
     * +------------------------------------------------------------+
     * |                        Ruby runtime                        |
     * +------------------------------------------------------------+
     * +------------------------------------------------------------+
     * |                     ScriptingContainer                     |
     * +------------------------------------------------------------+
     * +------------------+ +------------------+ +------------------+
     * |   Java Thread    | |   Java Thread    | |   Java Thread    |
     * +------------------+ +------------------+ +------------------+
     * +------------------------------------------------------------+
     * |                         JVM                                |
     * +------------------------------------------------------------+
     * @return A new RubyInterpreter with concurrent scope.
     */
    public static RubyInterpreter concurrent() {
        return new RubyInterpreter(LocalContextScope.CONCURRENT);
    }

    /**
     * +------------------------------------------------------------+
     * |                       Variable Map                         |
     * +------------------------------------------------------------+
     * +------------------------------------------------------------+
     * |                       Ruby runtime                         |
     * +------------------------------------------------------------+
     * +------------------+ +------------------+ +------------------+
     * |ScriptingContainer| |ScriptingContainer| |ScriptingContainer|
     * +------------------+ +------------------+ +------------------+
     * +------------------------------------------------------------+
     * |                         JVM                                |
     * +------------------------------------------------------------+
     * @return A new RubyInterpreter with singleton scope.
     */
    public static RubyInterpreter singleton() {
        return new RubyInterpreter(LocalContextScope.SINGLETON);
    }

    /**
     * +------------------+ +------------------+ +------------------+
     * |   Variable Map   | |   Variable Map   | |   Variable Map   |
     * +------------------+ +------------------+ +------------------+
     * +------------------+ +------------------+ +------------------+
     * |   Ruby runtime   | |   Ruby runtime   | |   Ruby runtime   |
     * +------------------+ +------------------+ +------------------+
     * +------------------------------------------------------------+
     * |                     ScriptingContainer                     |
     * +------------------------------------------------------------+
     * +------------------+ +------------------+ +------------------+
     * |   Java Thread    | |   Java Thread    | |   Java Thread    |
     * +------------------+ +------------------+ +------------------+
     * +------------------------------------------------------------+
     * |                         JVM                                |
     * +------------------------------------------------------------+
     * @return A new RubyInterpreter with thread safety scope.
     */
    public static RubyInterpreter threadSafe() {
        return new RubyInterpreter(LocalContextScope.THREADSAFE);
    }

    /**
     * Initialize the ruby engine.
     *
     * @throws ScriptException If the initialization failed.
     */
    public void initialize() throws ScriptException {
        this.runCommand("require 'java'");
    }

    /**
     * Register a class to be recognized by Ruby.
     *
     * @param clazz Class to register.
     * @throws ScriptException If the class cannot be registered.
     */
    public void registerClass(@SuppressWarnings("rawtypes") final Class clazz) throws ScriptException {
        String packageName = clazz.getPackage().getName();
        String className = clazz.getSimpleName();
        this.runCommand("java_import Java::" + packageName + "::" + className);
    }

    @Override
    public void setOutput(final Writer output) {
        this.container.setOutput(output);
    }

    @Override
    public ParsedScript runScript(final String file) throws ScriptException {
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(new File(file)))) {
            return this.container.parse(stream, file)::run;
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object runCommand(final String command) throws ScriptException {
        try {
            return this.container.runScriptlet(command);
        } catch (RaiseException | EvalFailedException | ParseFailedException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object getClassMethods(final Class<?> classToGet) {
        try {
            return this.runCommand(classToGet.getName() + ".java_class.declared_instance_methods");
        } catch (ScriptException e) {
            this.logger.error("Script error", e);
            return e.getMessage();
        }

    }

    @Override
    public void print(final String toPrint) {
        try {
            this.runCommand("puts '" + toPrint + "';");
        } catch (ScriptException e) {
            this.logger.error("Script error", e);
        }
    }

    @Override
    public String getFileHeader() {
        return "#!//usr//bin//ruby\n";
    }

    /**
     * @return rb
     */
    @Override
    public String getFileExtension() {
        return "rb";
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
        this.container.terminate();
    }
}
