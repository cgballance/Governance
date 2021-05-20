package com.webforged.enforcer.management.util;

import static java.time.Instant.ofEpochMilli;
import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneId.systemDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

public class Jsr310NullConverters {
	public static Collection<Converter<?, ?>> getConvertersToRegister() {

		List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(DateToLocalDateTimeConverter.INSTANCE);
		converters.add(LocalDateTimeToDateConverter.INSTANCE);
		converters.add(DateToLocalDateConverter.INSTANCE);
		converters.add(LocalDateToDateConverter.INSTANCE);
		converters.add(DateToLocalTimeConverter.INSTANCE);
		converters.add(LocalTimeToDateConverter.INSTANCE);
		converters.add(DateToInstantConverter.INSTANCE);
		converters.add(InstantToDateConverter.INSTANCE);
		converters.add(LocalDateTimeToInstantConverter.INSTANCE);
		converters.add(InstantToLocalDateTimeConverter.INSTANCE);
		/**
		converters.add(ZoneIdToStringConverter.INSTANCE);
		converters.add(StringToZoneIdConverter.INSTANCE);
		converters.add(DurationToStringConverter.INSTANCE);
		converters.add(StringToDurationConverter.INSTANCE);
		converters.add(PeriodToStringConverter.INSTANCE);
		converters.add(StringToPeriodConverter.INSTANCE);
		converters.add(StringToLocalDateConverter.INSTANCE);
		converters.add(StringToLocalDateTimeConverter.INSTANCE);
		converters.add(StringToInstantConverter.INSTANCE);
		**/
		
		return converters;
	}
	
	@ReadingConverter
	public static enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {

		INSTANCE;

		@Override
		public LocalDateTime convert(Date source) {
			if( source == null ) { return null; }
			return ofInstant(source.toInstant(), systemDefault());
		}
	}

	@WritingConverter
	public static enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {

		INSTANCE;

		@Override
		public Date convert(LocalDateTime source) {
			if( source == null ) { return null; }
			return Date.from(source.atZone(systemDefault()).toInstant());
		}
	}

	@ReadingConverter
	public static enum DateToLocalDateConverter implements Converter<Date, LocalDate> {

		INSTANCE;

		@Override
		public LocalDate convert(Date source) {
			if( source == null ) { return null; }
			return ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalDate();
		}
	}

	@WritingConverter
	public static enum LocalDateToDateConverter implements Converter<LocalDate, Date> {

		INSTANCE;

		@Override
		public Date convert(LocalDate source) {
			if( source == null ) { return null; }
			return Date.from(source.atStartOfDay(systemDefault()).toInstant());
		}
	}

	@ReadingConverter
	public static enum DateToLocalTimeConverter implements Converter<Date, LocalTime> {

		INSTANCE;

		@Override
		public LocalTime convert(Date source) {
			if( source == null ) { return null; }
			return ofInstant(ofEpochMilli(source.getTime()), systemDefault()).toLocalTime();
		}
	}

	@WritingConverter
	public static enum LocalTimeToDateConverter implements Converter<LocalTime, Date> {

		INSTANCE;

		@Override
		public Date convert(LocalTime source) {
			if( source == null ) { return null; }
			return Date.from(source.atDate(LocalDate.now()).atZone(systemDefault()).toInstant());
		}
	}

	@ReadingConverter
	public static enum DateToInstantConverter implements Converter<Date, Instant> {

		INSTANCE;

		@Override
		public Instant convert(Date source) {
			if( source == null ) { return null; }
			return source.toInstant();
		}
	}

	@WritingConverter
	public static enum InstantToDateConverter implements Converter<Instant, Date> {

		INSTANCE;

		@Override
		public Date convert(Instant source) {
			if( source == null ) { return null; }
			return Date.from(source);
		}
	}

	@ReadingConverter
	public static enum LocalDateTimeToInstantConverter implements Converter<LocalDateTime, Instant> {

		INSTANCE;

		@Override
		public Instant convert(LocalDateTime source) {
			if( source == null ) { return null; }
			return source.atZone(systemDefault()).toInstant();
		}
	}

	@ReadingConverter
	public static enum InstantToLocalDateTimeConverter implements Converter<Instant, LocalDateTime> {

		INSTANCE;

		@Override
		public LocalDateTime convert(Instant source) {
			if( source == null ) { return null; }
			return LocalDateTime.ofInstant(source, systemDefault());
		}
	}
	
	@ReadingConverter
	public static enum LocalDateTimeToOffsetDateTimeConverter implements Converter<LocalDateTime, OffsetDateTime> {
		INSTANCE;
	
		@Override
		public OffsetDateTime convert(LocalDateTime source) {
			if( source == null ) { return null; }
			return OffsetDateTime.of(source, ZoneOffset.UTC) ;
		}
	}
	
	@ReadingConverter
	public static enum InstantToOffsetDateTimeConverter implements Converter<Instant, OffsetDateTime> {
		INSTANCE;
	
		@Override
		public OffsetDateTime convert(Instant source) {
			if( source == null ) { return null; }
			return source.atOffset( ZoneOffset.UTC ) ;
		}
	}
	
	@ReadingConverter
	public static enum OffsetDateTimeToLocalDateTimeConverter implements Converter<OffsetDateTime, LocalDateTime> {
		INSTANCE;
	
		@Override
		public LocalDateTime convert(OffsetDateTime source) {
			if( source == null ) { return null; }
			return source.toLocalDateTime();
		}
	}

	@ReadingConverter
	public static enum OffsetDateTimeToInstantConverter implements Converter<OffsetDateTime, Instant> {
		INSTANCE;
	
		@Override
		public Instant convert(OffsetDateTime source) {
			if( source == null ) { return null; }
			return source.toInstant();
		}
	}
	
}
