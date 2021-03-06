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
package org.thingsplode.synapse.proxy;

import org.thingsplode.synapse.core.Command;
import org.thingsplode.synapse.core.CommandResult;

/**
 *
 * @author Csaba Tamas
 */
public abstract class AbstractCommandSink {

    public CommandResult processCommand(Command command) {
        try {
            return execute(command);
        } catch (Throwable th) {
            CommandResult cr = new CommandResult(command, CommandResult.ResultCode.FAILED);
            cr.setBody(th.getMessage());
            return cr;
        }
    }

    public abstract CommandResult execute(Command command);
}
