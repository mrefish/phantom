package com.websudos.phantom.builder.serializers

import java.util.concurrent.TimeUnit

import com.twitter.conversions.storage._
import com.twitter.util.{Duration => TwitterDuration}
import com.websudos.phantom.builder.query.QueryBuilderTest
import com.websudos.phantom.builder.syntax.CQLSyntax
import com.websudos.phantom.dsl._
import com.websudos.phantom.tables.{BasicTable, StaticTableTest}
import org.joda.time.Seconds

import scala.concurrent.duration._


class AlterQueryBuilderTest extends QueryBuilderTest {

  "The ALTER query builder" - {

    "should serialise ALTER .. ADD queries" - {
      "serialise an ADD query for a column without a STATIC modifier" in {
        val qb = BasicTable.alter.add("test_big_decimal", CQLSyntax.Types.Decimal).queryString
        qb shouldEqual s"ALTER TABLE phantom.BasicTable ADD test_big_decimal ${CQLSyntax.Types.Decimal}"
      }

      "serialise an ADD query for a column with a STATIC modifier" in {
        val qb = BasicTable.alter.add("test_big_decimal", CQLSyntax.Types.Decimal, static = true).queryString
        qb shouldEqual s"ALTER TABLE phantom.BasicTable ADD test_big_decimal ${CQLSyntax.Types.Decimal} STATIC"
      }

      "serialise an ADD query for a column without a STATIC modifier from a CQLQuery" in {
        val qb = BasicTable.alter.add(BasicTable.placeholder.qb).queryString
        qb shouldEqual s"ALTER TABLE phantom.BasicTable ADD placeholder ${CQLSyntax.Types.Text}"
      }

      "serialise an ADD query for a column with a STATIC modifier from a CQLQuery" in {
        val qb = BasicTable.alter.add(StaticTableTest.staticTest.qb).queryString
        qb shouldEqual s"ALTER TABLE phantom.BasicTable ADD staticTest ${CQLSyntax.Types.Text} STATIC"
      }
    }

    "should serialise ALTER .. DROP queries" - {
      "should serialise a DROP query based based on a column select" in {
        val qb = BasicTable.alter.drop(_.placeholder).queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable DROP placeholder"
      }

      "should serialise a DROP query based on string value" in {
        val qb = BasicTable.alter.drop("test").queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable DROP test"
      }

      "should not compile DROP queries on INDEX fields" in {
        """
          | BasicTable.alter.drop(_.id).queryString
        """ shouldNot compile
      }
    }

    "should serialise ALTER .. WITH queries" - {

      "serialise a simple create query with a SizeTieredCompactionStrategy and no compaction strategy options set" in {

        val qb = BasicTable.alter.`with`(compaction eqs SizeTieredCompactionStrategy).qb.queryString

        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH compaction = { 'class' " +
          ": 'SizeTieredCompactionStrategy' }"
      }

      "serialise a simple create query with a SizeTieredCompactionStrategy and 1 compaction strategy options set" in {

        val qb = BasicTable.alter.`with`(compaction eqs SizeTieredCompactionStrategy.sstable_size_in_mb(50.megabytes)).qb.queryString

        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH compaction = { 'class' " +
          ": 'SizeTieredCompactionStrategy', 'sstable_size_in_mb' : '50.0 MiB' }"
      }

      "serialise a simple create query with a SizeTieredCompactionStrategy and 1 compaction strategy options set and a compression strategy set" in {

        val qb = BasicTable.alter
          .`with`(compaction eqs SizeTieredCompactionStrategy.sstable_size_in_mb(50.megabytes))
          .and(compression eqs LZ4Compressor.crc_check_chance(0.5))
          .qb.queryString

        qb shouldEqual """ALTER TABLE phantom.BasicTable WITH compaction = { 'class' : 'SizeTieredCompactionStrategy', 'sstable_size_in_mb' : '50.0 MiB' } AND compression = { 'sstable_compression' : 'LZ4Compressor', 'crc_check_chance' : 0.5 }"""
      }

      "add a comment option to a create query" in {
        val qb = BasicTable.alter
          .`with`(comment eqs "testing")
          .qb.queryString

        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH comment = 'testing'"
      }

      "allow specifying a read_repair_chance clause" in {
        val qb = BasicTable.alter.`with`(read_repair_chance eqs 5D).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH read_repair_chance = 5.0"
      }

      "allow specifying a dclocal_read_repair_chance clause" in {
        val qb = BasicTable.alter.`with`(dclocal_read_repair_chance eqs 5D).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH dclocal_read_repair_chance = 5.0"
      }

      "allow specifying a replicate_on_write clause" in {
        val qb = BasicTable.alter.`with`(replicate_on_write eqs true).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH replicate_on_write = true"
      }

      "allow specifying a custom gc_grace_seconds clause" in {
        val qb = BasicTable.alter.`with`(gc_grace_seconds eqs 5.seconds).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH gc_grace_seconds = 5"
      }

      "allow specifying larger custom units as gc_grace_seconds" in {
        val qb = BasicTable.alter.`with`(gc_grace_seconds eqs 1.day).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH gc_grace_seconds = 86400"
      }

      "allow specifying larger custom units as gc_grace_seconds using the Twitter conversions API" in {
        val qb = BasicTable.alter.`with`(gc_grace_seconds eqs TwitterDuration.fromSeconds(86400)).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH gc_grace_seconds = 86400"
      }

      "allow specifying custom gc_grade_seconds using the Joda Time ReadableInstant and Second API" in {
        val qb = BasicTable.alter.`with`(gc_grace_seconds eqs Seconds.seconds(86400)).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH gc_grace_seconds = 86400"
      }

      "allow specifying a bloom_filter_fp_chance using a Double param value" in {
        val qb = BasicTable.alter.`with`(bloom_filter_fp_chance eqs 5D).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH " +
          "bloom_filter_fp_chance = 5.0"
      }
    }

    "should allow specifying cache strategies " - {
      "specify Cache.None as a cache strategy" in {
        val qb = BasicTable.alter.`with`(caching eqs Cache.None).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH caching = 'none'"
      }

      "specify Cache.KeysOnly as a caching strategy" in {
        val qb = BasicTable.alter.`with`(caching eqs Cache.KeysOnly).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH caching = 'keys_only'"
      }
    }

    "should allow specifying a default_time_to_live" - {
      "specify a default time to live using a Long value" in {
        val qb = BasicTable.alter.`with`(default_time_to_live eqs 500L).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH default_time_to_live = 500"
      }

      "specify a default time to live using a org.joda.time.Seconds value" in {
        val qb = BasicTable.alter.`with`(default_time_to_live eqs Seconds.seconds(500)).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH default_time_to_live = 500"
      }

      "specify a default time to live using a scala.concurrent.duration.FiniteDuration value" in {
        val qb = BasicTable.alter.`with`(default_time_to_live eqs FiniteDuration(500, TimeUnit.SECONDS)).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH default_time_to_live = 500"
      }

      "specify a default time to live using a com.twitter.util.Duration value" in {
        val qb = BasicTable.alter.`with`(default_time_to_live eqs com.twitter.util.Duration.fromSeconds(500)).qb.queryString
        qb shouldEqual "ALTER TABLE phantom.BasicTable WITH default_time_to_live = 500"
      }
    }
  }




}
