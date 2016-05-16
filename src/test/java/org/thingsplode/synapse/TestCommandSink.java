/*
 * Copyright 2016 Csaba Tamas.
 *
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
 */
package org.thingsplode.synapse;

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsplode.synapse.core.Command;
import org.thingsplode.synapse.core.CommandResult;
import org.thingsplode.synapse.proxy.AbstractCommandSink;

/**
 *
 * @author Csaba Tamas
 */
public class TestCommandSink extends AbstractCommandSink {

    private final Logger logger = LoggerFactory.getLogger(TestCommandSink.class);
    public static ArrayBlockingQueue<Command<Serializable>> commandQueue = new ArrayBlockingQueue<>(1000);

    @Override
    public CommandResult execute(Command command) {
        logger.debug("COMMAND@TestCommandSink RECEIVED --> " + command.toString());
        try {
            commandQueue.put(command);
        } catch (InterruptedException ex) {
            logger.error("EXCEPTION WHILE ADDING TO THE QUEUE --> " + ex.getMessage());
        }
        CommandResult cr = new CommandResult(command, CommandResult.ResultCode.EXECUTED);
        return cr;
    }

}
