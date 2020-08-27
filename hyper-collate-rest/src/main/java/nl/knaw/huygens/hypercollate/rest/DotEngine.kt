package nl.knaw.huygens.hypercollate.rest

/*-
 * #%L
 * hyper-collate-rest
 * =======
 * Copyright (C) 2017 - 2020 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DotEngine(private val dotPath: String) {
    private val processThreads = Executors.newCachedThreadPool()

    //    @Throws(IOException::class)
    fun renderAs(format: String, dot: String, outputStream: OutputStream) {
        val dotProc = ProcessBuilder(dotPath, "-T$format").start()
        val errors = StringWriter()
//        try {
        CompletableFuture.allOf(
                processErrorStream(dotProc, errors),
                processOutputStream(dotProc, dot),
                processInputStream(dotProc, outputStream),
                waitForCompletion(dotProc, errors))
//                    .exceptionally { t: Throwable? -> throw WebApplicationException(t) }
                .get()
//        } catch (e: InterruptedException) {
//            throw WebApplicationException(e)
//        } catch (e: ExecutionException) {
//            throw WebApplicationException(e)
//        }
    }

    private fun waitForCompletion(dotProc: Process, errors: StringWriter): CompletableFuture<Void> =
            CompletableFuture.runAsync(
                    {
                        try {
                            if (!dotProc.waitFor(2, TimeUnit.MINUTES)) {
                                throw CompletionException(IllegalStateException(errors.toString()))
                            }
                        } catch (e: InterruptedException) {
                            throw CompletionException(e)
                        }
                    },
                    processThreads)

    private fun processInputStream(dotProc: Process, outputStream: OutputStream): CompletableFuture<Void> =
            CompletableFuture.runAsync(
                    {
                        val buf = ByteArray(8192)
                        try {
                            dotProc.inputStream.use { `in` ->
                                outputStream.use { out ->
                                    var len: Int
                                    while (`in`.read(buf).also { len = it } >= 0) {
                                        out.write(buf, 0, len)
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            throw CompletionException(e)
                        }
                    },
                    processThreads)

    private fun processOutputStream(dotProc: Process, dot: String): CompletableFuture<Void> =
            CompletableFuture.runAsync(
                    {
                        try {
                            OutputStreamWriter(dotProc.outputStream, StandardCharsets.UTF_8).use { dotProcStream -> dotProcStream.write(dot) }
                        } catch (e: IOException) {
                            throw CompletionException(e)
                        }
                    },
                    processThreads)

    private fun processErrorStream(dotProc: Process, errors: StringWriter): CompletableFuture<Void> =
            CompletableFuture.runAsync(
                    {
                        val buf = CharArray(8192)
                        try {
                            InputStreamReader(dotProc.errorStream).use { errorStream ->
                                var len: Int
                                while (errorStream.read(buf).also { len = it } >= 0) {
                                    errors.write(buf, 0, len)
                                }
                            }
                        } catch (e: IOException) {
                            throw CompletionException(e)
                        }
                    },
                    processThreads)
}