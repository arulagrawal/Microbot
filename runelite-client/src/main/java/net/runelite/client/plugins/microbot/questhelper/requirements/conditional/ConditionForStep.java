/*
 * Copyright (c) 2020, Zoinkwiz <https://github.com/Zoinkwiz>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.microbot.questhelper.requirements.conditional;

import net.runelite.client.plugins.microbot.questhelper.requirements.Requirement;
import net.runelite.client.plugins.microbot.questhelper.requirements.util.LogicType;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class ConditionForStep implements InitializableRequirement
{
	@Setter
	@Getter
	protected boolean hasPassed;
	protected boolean onlyNeedToPassOnce;
	protected LogicType logicType;

	@Getter
	protected List<Requirement> conditions = new ArrayList<>();

	@Override
	abstract public boolean check(Client client);

	@Override
	public void initialize(Client client)
	{
		conditions.stream()
			.filter(InitializableRequirement.class::isInstance)
			.forEach(req -> ((InitializableRequirement) req).initialize(client));
	}

	@Override
	public void updateHandler()
	{
		conditions.stream()
			.filter(InitializableRequirement.class::isInstance)
			.forEach(req -> ((InitializableRequirement) req).updateHandler());
	}

	@Setter
	private String text = "";

	@Nonnull
	@Override
	public String getDisplayText()
	{
		return this.text;
	}
}
