/*
 * Copyright (c) 2026, Quest Helper contributors
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
package com.questhelper.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides translations for quest helper strings using the "English-as-key" pattern.
 * <p>
 * The English text lives in the code: {@code translationService.t("Get a bucket")}.
 * Translations are looked up in {@code messages_<lang>.properties} (e.g.
 * {@code messages_es.properties}); {@code messages.properties} holds optional
 * English entries. When no translation exists, the English input text is
 * returned, so untranslated quests work out of the box and a missing entry
 * never shows a raw internal key.
 * <p>
 * The message files use a simple {@code key=value} format parsed as UTF-8,
 * where the key is everything before the first {@code =}. Unlike
 * {@link java.util.Properties}, spaces in keys do not need escaping, so keys
 * can be the exact English strings from the code. Blank lines and lines
 * starting with {@code #} or {@code !} are ignored.
 * <p>
 * Translation conventions:
 * <ul>
 * <li>Quest names are never translated; do not wrap them in {@link #t(String)}.</li>
 * <li>Game entity names (items, NPCs, locations) are never translated; keep them
 * in English inside both the source string and the translation.</li>
 * <li>Dialog choices ({@code addDialogStep}) and requirement checks that match
 * live game text are never translated, as they assume an English game client.</li>
 * <li>Only translate user-facing instructional text: step texts, tooltips,
 * panel section titles, combat requirements and unlock rewards.</li>
 * </ul>
 * <p>
 * To translate a quest: wrap its user-facing strings with {@link #t(String)} and
 * add the matching entries to each {@code messages_<lang>.properties} file.
 * To add a new language: add a constant to {@link Language} and create its
 * {@code messages_<lang>.properties} file.
 * <p>
 * Injected via Guice as a {@code @Singleton}; changing the language at runtime
 * applies to strings resolved from that point on.
 */
@Slf4j
@Singleton
public class TranslationService
{
	/**
	 * English messages. Also acts as the fallback for languages that have no
	 * entry for a given key.
	 */
	private final Map<String, String> englishMessages = loadMessages(Language.ENGLISH);

	/**
	 * Messages for the currently selected language. Volatile because the
	 * language may be changed from the config thread while other threads
	 * translate.
	 */
	private volatile Map<String, String> currentMessages = englishMessages;

	@Getter
	private volatile Language language = Language.ENGLISH;

	public void setLanguage(Language language)
	{
		this.language = language;
		if (language == Language.ENGLISH)
		{
			this.currentMessages = englishMessages;
		}
		else
		{
			this.currentMessages = loadMessages(language);
		}
	}

	/**
	 * Translates the given text to the currently configured language.
	 * <p>
	 * Fallback chain: current language messages, then the English messages,
	 * then the input text itself. With the English-as-key pattern the input is
	 * already English, so a missing translation simply displays English.
	 *
	 * @param text English text (quest strings) or a UI label key
	 * @return the translation, or {@code text} itself as a last resort
	 */
	public String t(String text)
	{
		if (text == null)
		{
			return null;
		}

		String translated = currentMessages.get(text);
		if (translated != null)
		{
			return translated;
		}

		if (currentMessages != englishMessages)
		{
			translated = englishMessages.get(text);
			if (translated != null)
			{
				return translated;
			}
		}

		return text;
	}

	private static Map<String, String> loadMessages(Language language)
	{
		String resource = language.getMessagesResource();
		try (InputStream in = TranslationService.class.getResourceAsStream(resource))
		{
			if (in == null)
			{
				return Collections.emptyMap();
			}

			Map<String, String> messages = new HashMap<>();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					if (line.isBlank() || line.startsWith("#") || line.startsWith("!"))
					{
						continue;
					}

					int separator = line.indexOf('=');
					if (separator <= 0)
					{
						continue;
					}

					String key = line.substring(0, separator);
					String value = line.substring(separator + 1);
					// Strip leading whitespace from the value, as Properties does
					value = value.stripLeading();
					messages.put(key, value);
				}
			}
			return messages;
		}
		catch (IOException e)
		{
			log.warn("Failed to load {} messages from {}", language, resource, e);
			return Collections.emptyMap();
		}
		catch (UncheckedIOException e)
		{
			log.warn("Failed to load {} messages from {}", language, resource, e.getCause());
			return Collections.emptyMap();
		}
	}
}
