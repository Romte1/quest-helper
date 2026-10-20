package com.questhelper.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

// Tests for TranslationService, focused on the fallback chain
// (current language bundle -> English bundle -> input text as-is).
public class TranslationServiceTest
{
	// English mode passes the input through unchanged (English-as-key pattern).
	@Test
	public void englishModePassesInputThrough()
	{
		var service = new TranslationService();
		assertEquals("Get a bucket", service.t("Get a bucket"));
	}

	// Spanish mode resolves a translated quest string.
	@Test
	public void spanishModeResolvesTranslatedQuestString()
	{
		var service = new TranslationService();
		service.setLanguage(Language.SPANISH);
		assertEquals("Compra un Bucket en la General Store de Lumbridge.",
			service.t("Purchase a bucket from the Lumbridge General Store."));
	}

	// An untranslated quest string falls back to the English input text itself,
	// never to a raw internal key.
	@Test
	public void spanishModeFallsBackToEnglishTextNotAKey()
	{
		var service = new TranslationService();
		service.setLanguage(Language.SPANISH);
		assertEquals("A future quest step not yet translated",
			service.t("A future quest step not yet translated"));
	}

	// Changing language at runtime takes effect immediately on the next call.
	@Test
	public void languageChangeAppliesImmediately()
	{
		var service = new TranslationService();
		assertEquals("Starting off", service.t("Starting off"));
		service.setLanguage(Language.SPANISH);
		assertEquals("Empezando", service.t("Starting off"));
		service.setLanguage(Language.ENGLISH);
		assertEquals("Starting off", service.t("Starting off"));
	}

	// Quest names must never be translated: no entry exists for them in any
	// bundle, so they pass through unchanged even in Spanish mode.
	@Test
	public void questNamesAreNeverTranslated()
	{
		var service = new TranslationService();
		service.setLanguage(Language.SPANISH);
		assertEquals("Cook's Assistant", service.t("Cook's Assistant"));
		assertEquals("Imp Catcher", service.t("Imp Catcher"));
		assertEquals("The Restless Ghost", service.t("The Restless Ghost"));
		assertEquals("Pirate's Treasure", service.t("Pirate's Treasure"));
		assertEquals("Rune Mysteries", service.t("Rune Mysteries"));
	}

	// Null input stays null instead of throwing.
	@Test
	public void nullInputReturnsNull()
	{
		var service = new TranslationService();
		assertNull(service.t((String) null));
	}
}
