/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.range.LocalDateRange;

/**
 * A holiday calendar implementation based on an immutable set of holiday dates and weekends.
 * <p>
 * A standard immutable implementation of {@link HolidayCalendar} that stores all
 * dates that are holidays, plus a list of weekend days.
 * Internally, the class uses a range to determine the valid range of dates.
 * Only dates between the start of the earliest year and the end of the latest year can be queried.
 * <p>
 * In most cases, applications should refer to calendars by name, using {@link HolidayCalendar#of(String)}.
 * The named calendar will typically be resolved to an instance of this class.
 * As such, it is recommended to use the {@code HolidayCalendar} interface in application
 * code rather than directly referring to this class.
 */
@BeanDefinition(builderScope = "private")
public final class StandardHolidayCalendar
    implements HolidayCalendar, ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The calendar name.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final String name;
  /**
   * The set of holiday dates.
   * <p>
   * Each date in this set is not a business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSortedSet<LocalDate> holidays;
  /**
   * The set of weekend days.
   * <p>
   * Each date that has a day-of-week matching one of these days is not a business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSet<DayOfWeek> weekendDays;
  /**
   * The supported range of dates.
   */
  private final LocalDateRange range;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code HolidayCalendar} from a set of holiday dates and weekend days.
   * <p>
   * The holiday dates will be extracted into a set with duplicates ignored.
   * The minimum supported date for query is the start of the year of the earliest holiday.
   * The maximum supported date for query is the end of the year of the latest holiday.
   * <p>
   * The weekend days may both be the same.
   * 
   * @param name  the calendar name
   * @param holidays  the set of holiday dates
   * @param firstWeekendDay  the first weekend day
   * @param secondWeekendDay  the second weekend day, may be same as first
   * @return the holiday calendar
   */
  public static StandardHolidayCalendar of(
      String name, Iterable<LocalDate> holidays, DayOfWeek firstWeekendDay, DayOfWeek secondWeekendDay) {
    ArgChecker.notNull(name, "name");
    ArgChecker.noNulls(holidays, "holidays");
    ArgChecker.notNull(firstWeekendDay, "firstWeekendDay");
    ArgChecker.notNull(secondWeekendDay, "secondWeekendDay");
    ImmutableSet<DayOfWeek> weekendDays = Sets.immutableEnumSet(firstWeekendDay, secondWeekendDay);
    return new StandardHolidayCalendar(name, ImmutableSortedSet.copyOf(holidays), weekendDays);
  }

  /**
   * Obtains a {@code HolidayCalendar} from a set of holiday dates and weekend days.
   * <p>
   * The holiday dates will be extracted into a set with duplicates ignored.
   * The minimum supported date for query is the start of the year of the earliest holiday.
   * The maximum supported date for query is the end of the year of the latest holiday.
   * <p>
   * The weekend days may be empty, in which case the holiday dates should contain any weekends.
   * 
   * @param name  the calendar name
   * @param holidays  the set of holiday dates
   * @param weekendDays  the days that define the weekend, if empty then weekends are treated as business days
   * @return the holiday calendar
   */
  public static StandardHolidayCalendar of(
      String name, Iterable<LocalDate> holidays, Iterable<DayOfWeek> weekendDays) {
    ArgChecker.notNull(name, "name");
    ArgChecker.noNulls(holidays, "holidays");
    ArgChecker.noNulls(weekendDays, "weekendDays");
    return new StandardHolidayCalendar(name, ImmutableSortedSet.copyOf(holidays), Sets.immutableEnumSet(weekendDays));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance calculating the supported range.
   * 
   * @param name  the calendar name
   * @param holidays  the set of holidays, validated non-null
   * @param weekendDays  the set of weekend days, validated non-null
   */
  @ImmutableConstructor
  private StandardHolidayCalendar(String name, SortedSet<LocalDate> holidays, Set<DayOfWeek> weekendDays) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(holidays, "holidays");
    ArgChecker.notNull(weekendDays, "weekendDays");
    this.name = name;
    this.holidays = ImmutableSortedSet.copyOfSorted(holidays);
    this.weekendDays = Sets.immutableEnumSet(weekendDays);
    if (holidays.isEmpty()) {
      range = LocalDateRange.ALL;
    } else {
      this.range = LocalDateRange.ofClosed(
          holidays.first().with(TemporalAdjusters.firstDayOfYear()),
          holidays.last().with(TemporalAdjusters.lastDayOfYear()));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the range of dates that may be queried.
   * <p>
   * If a holiday query is made for a date outside this range, an exception is thrown.
   * 
   * @return the range of dates that may be queried
   */
  public LocalDateRange getRange() {
    return range;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean isHoliday(LocalDate date) {
    ArgChecker.notNull(date, "date");
    if (range.contains(date) == false) {
      throw new IllegalArgumentException("Date is not within the range of known holidays: " + date + ", " + range);
    }
    return holidays.contains(date) || weekendDays.contains(date.getDayOfWeek());
  }

  @Override
  public HolidayCalendar combineWith(HolidayCalendar other) {
    ArgChecker.notNull(other, "other");
    if (this.equals(other)) {
      return this;
    }
    if (other == HolidayCalendars.NONE) {
      return this;
    }
    if (other instanceof StandardHolidayCalendar) {
      StandardHolidayCalendar otherCal = (StandardHolidayCalendar) other;
      LocalDateRange newRange = range.union(otherCal.range);  // exception if no overlap
      ImmutableSortedSet<LocalDate> newHolidays =
          ImmutableSortedSet.copyOf(Iterables.concat(holidays, otherCal.holidays))
              .subSet(newRange.getStart(), newRange.getEndExclusive());
      ImmutableSet<DayOfWeek> newWeekends = ImmutableSet.copyOf(Iterables.concat(weekendDays, otherCal.weekendDays));
      String combinedName = name + "+" + otherCal.name;
      return new StandardHolidayCalendar(combinedName, newHolidays, newWeekends);
    }
    return HolidayCalendar.super.combineWith(other);
  }

  //-----------------------------------------------------------------------
  /**
   * Returns the name of the calendar.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code StandardHolidayCalendar}.
   * @return the meta-bean, not null
   */
  public static StandardHolidayCalendar.Meta meta() {
    return StandardHolidayCalendar.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(StandardHolidayCalendar.Meta.INSTANCE);
  }

  @Override
  public StandardHolidayCalendar.Meta metaBean() {
    return StandardHolidayCalendar.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calendar name.
   * @return the value of the property, not null
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the set of holiday dates.
   * <p>
   * Each date in this set is not a business day.
   * @return the value of the property, not null
   */
  public ImmutableSortedSet<LocalDate> getHolidays() {
    return holidays;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the set of weekend days.
   * <p>
   * Each date that has a day-of-week matching one of these days is not a business day.
   * @return the value of the property, not null
   */
  public ImmutableSet<DayOfWeek> getWeekendDays() {
    return weekendDays;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      StandardHolidayCalendar other = (StandardHolidayCalendar) obj;
      return JodaBeanUtils.equal(getName(), other.getName()) &&
          JodaBeanUtils.equal(getHolidays(), other.getHolidays()) &&
          JodaBeanUtils.equal(getWeekendDays(), other.getWeekendDays());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getName());
    hash += hash * 31 + JodaBeanUtils.hashCode(getHolidays());
    hash += hash * 31 + JodaBeanUtils.hashCode(getWeekendDays());
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code StandardHolidayCalendar}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", StandardHolidayCalendar.class, String.class);
    /**
     * The meta-property for the {@code holidays} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSortedSet<LocalDate>> holidays = DirectMetaProperty.ofImmutable(
        this, "holidays", StandardHolidayCalendar.class, (Class) ImmutableSortedSet.class);
    /**
     * The meta-property for the {@code weekendDays} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSet<DayOfWeek>> weekendDays = DirectMetaProperty.ofImmutable(
        this, "weekendDays", StandardHolidayCalendar.class, (Class) ImmutableSet.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "holidays",
        "weekendDays");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -510663909:  // holidays
          return holidays;
        case 563236190:  // weekendDays
          return weekendDays;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends StandardHolidayCalendar> builder() {
      return new StandardHolidayCalendar.Builder();
    }

    @Override
    public Class<? extends StandardHolidayCalendar> beanType() {
      return StandardHolidayCalendar.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code holidays} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSortedSet<LocalDate>> holidays() {
      return holidays;
    }

    /**
     * The meta-property for the {@code weekendDays} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableSet<DayOfWeek>> weekendDays() {
      return weekendDays;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((StandardHolidayCalendar) bean).getName();
        case -510663909:  // holidays
          return ((StandardHolidayCalendar) bean).getHolidays();
        case 563236190:  // weekendDays
          return ((StandardHolidayCalendar) bean).getWeekendDays();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code StandardHolidayCalendar}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<StandardHolidayCalendar> {

    private String name;
    private SortedSet<LocalDate> holidays = new TreeSet<LocalDate>();
    private Set<DayOfWeek> weekendDays = new HashSet<DayOfWeek>();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -510663909:  // holidays
          return holidays;
        case 563236190:  // weekendDays
          return weekendDays;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case -510663909:  // holidays
          this.holidays = (SortedSet<LocalDate>) newValue;
          break;
        case 563236190:  // weekendDays
          this.weekendDays = (Set<DayOfWeek>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public StandardHolidayCalendar build() {
      return new StandardHolidayCalendar(
          name,
          holidays,
          weekendDays);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("StandardHolidayCalendar.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("holidays").append('=').append(JodaBeanUtils.toString(holidays)).append(',').append(' ');
      buf.append("weekendDays").append('=').append(JodaBeanUtils.toString(weekendDays));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
