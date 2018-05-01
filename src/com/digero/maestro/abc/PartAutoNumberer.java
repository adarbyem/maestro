package com.digero.maestro.abc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.prefs.Preferences;

import com.digero.common.abc.LotroInstrument;

public class PartAutoNumberer
{
	public static class Settings
	{
		private Map<LotroInstrument, Integer> firstNumber;
		private boolean incrementByTen;

		private Settings(Preferences prefs)
		{
			firstNumber = new HashMap<LotroInstrument, Integer>(LotroInstrument.values().length);
			int i = 0;
			for (LotroInstrument instrument : LotroInstrument.values())
			{
				if (instrument != LotroInstrument.BASIC_LUTE && instrument != LotroInstrument.MISTY_MOUNTAIN_HARP
						&& instrument != LotroInstrument.PIBGORN && instrument != LotroInstrument.COWBELL
						&& instrument != LotroInstrument.MOOR_COWBELL && instrument != LotroInstrument.LONELY_MOUNTAIN_FIDDLE
						&& instrument != LotroInstrument.SPRIGHTLY_FIDDLE && instrument != LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE
						&& instrument != LotroInstrument.STUDENT_FIDDLE_NOFX)
				{
					i++;
					int defaultValue = i;
					if (instrument == LotroInstrument.LUTE_OF_AGES)
						defaultValue = prefs.getInt("Lute", i); // Handle the name change Lute => Lute of Ages

					firstNumber.put(instrument, prefs.getInt(instrument.toString(), defaultValue));
				}
			}

			// Basic Lute defaults to the Lute of Ages number
			firstNumber.put(LotroInstrument.BASIC_LUTE,
					prefs.getInt(LotroInstrument.BASIC_LUTE.toString(), firstNumber.get(LotroInstrument.LUTE_OF_AGES)));

			// Misty Mountain Harp defaults to the Harp number
			firstNumber
					.put(LotroInstrument.MISTY_MOUNTAIN_HARP,
							prefs.getInt(LotroInstrument.MISTY_MOUNTAIN_HARP.toString(),
									firstNumber.get(LotroInstrument.HARP)));

			// Pibgorn defaults to the Bagpipe number
			firstNumber.put(LotroInstrument.PIBGORN,
					prefs.getInt(LotroInstrument.PIBGORN.toString(), firstNumber.get(LotroInstrument.BAGPIPE)));

			// Cowbells defaults to the drum number
			firstNumber.put(LotroInstrument.COWBELL,
					prefs.getInt(LotroInstrument.COWBELL.toString(), firstNumber.get(LotroInstrument.DRUMS)));
			firstNumber.put(LotroInstrument.MOOR_COWBELL,
					prefs.getInt(LotroInstrument.MOOR_COWBELL.toString(), firstNumber.get(LotroInstrument.DRUMS)));
			
			// Fiddles defaults to Student's fiddle number
			firstNumber.put(LotroInstrument.LONELY_MOUNTAIN_FIDDLE,
					prefs.getInt(LotroInstrument.LONELY_MOUNTAIN_FIDDLE.toString(), firstNumber.get(LotroInstrument.STUDENT_FIDDLE)));
			firstNumber.put(LotroInstrument.SPRIGHTLY_FIDDLE,
					prefs.getInt(LotroInstrument.SPRIGHTLY_FIDDLE.toString(), firstNumber.get(LotroInstrument.STUDENT_FIDDLE)));
			firstNumber.put(LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE,
					prefs.getInt(LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE.toString(), firstNumber.get(LotroInstrument.STUDENT_FIDDLE)));
			firstNumber.put(LotroInstrument.STUDENT_FIDDLE_NOFX,
					prefs.getInt(LotroInstrument.STUDENT_FIDDLE_NOFX.toString(), firstNumber.get(LotroInstrument.STUDENT_FIDDLE)));

			incrementByTen = prefs.getBoolean("incrementByTen", true);
		}

		private void save(Preferences prefs)
		{
			for (Entry<LotroInstrument, Integer> entry : firstNumber.entrySet())
			{
				prefs.putInt(entry.getKey().toString(), entry.getValue());
			}
			prefs.putBoolean("incrementByTen", incrementByTen);
		}

		public Settings(Settings source)
		{
			copyFrom(source);
		}

		public void copyFrom(Settings source)
		{
			firstNumber = new HashMap<LotroInstrument, Integer>(source.firstNumber);
			incrementByTen = source.incrementByTen;
		}

		public int getIncrement()
		{
			return incrementByTen ? 10 : 1;
		}

		public boolean isIncrementByTen()
		{
			return incrementByTen;
		}

		public void setIncrementByTen(boolean incrementByTen)
		{
			this.incrementByTen = incrementByTen;
		}

		public void setFirstNumber(LotroInstrument instrument, int number)
		{
			firstNumber.put(instrument, number);
		}

		public int getFirstNumber(LotroInstrument instrument)
		{
			return firstNumber.get(instrument);
		}
	}

	private Settings settings;
	private Preferences prefsNode;
	private List<? extends NumberedAbcPart> parts = null;

	public PartAutoNumberer(Preferences prefsNode)
	{
		this.prefsNode = prefsNode;
		this.settings = new Settings(prefsNode);
	}

	public Settings getSettingsCopy()
	{
		return new Settings(settings);
	}

	public boolean isIncrementByTen()
	{
		return settings.isIncrementByTen();
	}

	public int getIncrement()
	{
		return settings.getIncrement();
	}

	public int getFirstNumber(LotroInstrument instrument)
	{
		return settings.getFirstNumber(instrument);
	}

	public void setSettings(Settings settings)
	{
		this.settings.copyFrom(settings);
		this.settings.save(prefsNode);
	}

	public void setParts(List<? extends NumberedAbcPart> parts)
	{
		this.parts = parts;
	}

	public void renumberAllParts()
	{
		if (parts == null)
			return;

		Set<Integer> numbersInUse = new HashSet<Integer>(parts.size());

		for (NumberedAbcPart part : parts)
		{
			int partNumber = getFirstNumber(part.getInstrument());
			while (numbersInUse.contains(partNumber))
			{
				partNumber += getIncrement();
			}
			numbersInUse.add(partNumber);
			part.setPartNumber(partNumber);
		}
	}

	public void onPartAdded(NumberedAbcPart partAdded)
	{
		if (parts == null)
			return;

		int newPartNumber = settings.getFirstNumber(partAdded.getInstrument());

		boolean conflict;
		do
		{
			conflict = false;
			for (NumberedAbcPart part : parts)
			{
				if (part != partAdded && part.getPartNumber() == newPartNumber)
				{
					newPartNumber += getIncrement();
					conflict = true;
				}
			}
		} while (conflict);

		partAdded.setPartNumber(newPartNumber);
	}

	public void onPartDeleted(NumberedAbcPart partDeleted)
	{
		if (parts == null)
			return;

		for (NumberedAbcPart part : parts)
		{
			int partNumber = part.getPartNumber();
			int deletedNumber = partDeleted.getPartNumber();
			int partFirstNumber = getFirstNumber(part.getInstrument());
			int deletedFirstNumber = getFirstNumber(partDeleted.getInstrument());
			if (part != partDeleted && partNumber > deletedNumber && partNumber > partFirstNumber
					&& partFirstNumber == deletedFirstNumber)
			{
				part.setPartNumber(partNumber - getIncrement());
			}
		}
	}

	public void setPartNumber(NumberedAbcPart partToChange, int newPartNumber)
	{
		if (parts == null)
			return;

		for (NumberedAbcPart part : parts)
		{
			if (part != partToChange && part.getPartNumber() == newPartNumber)
			{
				part.setPartNumber(partToChange.getPartNumber());
				break;
			}
		}
		partToChange.setPartNumber(newPartNumber);
	}

	public void setInstrument(NumberedAbcPart partToChange, LotroInstrument newInstrument)
	{
		if (newInstrument != partToChange.getInstrument())
		{
			onPartDeleted(partToChange);
			partToChange.setInstrument(newInstrument);
			onPartAdded(partToChange);
		}
	}

	public LotroInstrument[] getSortedInstrumentList()
	{
		LotroInstrument[] instruments = LotroInstrument.values();
		Arrays.sort(instruments, new Comparator<LotroInstrument>()
		{
			@Override public int compare(LotroInstrument a, LotroInstrument b)
			{
				int diff = getFirstNumber(a) - getFirstNumber(b);
				if (diff != 0)
					return diff;

				return a.toString().compareTo(b.toString());
			}
		});
		return instruments;
	}
}
