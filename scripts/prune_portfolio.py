#!/usr/bin/env python3
import os
import sys
import re
import argparse
import sqlite3
import shutil
from datetime import datetime

# Path resolution
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SANS_FINANCE_DIR = os.path.dirname(SCRIPT_DIR)
PROJECTS_DIR = os.path.dirname(SANS_FINANCE_DIR)

DEFAULT_DB_PATH = os.path.join(SANS_FINANCE_DIR, "sans_finance_db")
DEFAULT_GCS_BUCKET = os.getenv("PORTFOLIO_GCS_BUCKET", "ichsanul-portfolio-snapshots")

DATA_DIR = os.getenv("PORTFOLIO_DATA_DIR")
if not DATA_DIR:
    standard_path = os.path.join(PROJECTS_DIR, "portfolio-integration", "data")
    if os.path.exists(standard_path):
        DATA_DIR = standard_path
    else:
        DATA_DIR = "./portfolio_data"

SNAPSHOT_FILENAME_REGEX = re.compile(r"^(\d{4}-\d{2}-\d{2})_snapshot\.json$")


def get_gcs_client():
    """Initializes Google Cloud Storage client using service account or ADC."""
    try:
        from google.cloud import storage
    except ImportError:
        print("❌ Error: 'google-cloud-storage' package is not installed.")
        print("💡 Install it via: pip install google-cloud-storage")
        return None

    creds_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if not creds_path:
        candidate_paths = [
            os.path.join(SANS_FINANCE_DIR, "..", "creds", "gcp", "SA_cred_general.json"),
            os.path.join(SANS_FINANCE_DIR, "..", "creds", "gcp", "bq.json"),
            os.path.join(PROJECTS_DIR, "creds", "gcp", "SA_cred_general.json"),
            os.path.join(PROJECTS_DIR, "creds", "gcp", "bq.json"),
        ]
        for cp in candidate_paths:
            if os.path.exists(cp):
                creds_path = cp
                break

    if creds_path and os.path.exists(creds_path):
        print(f"🔑 Using GCS credentials: {creds_path}")
        return storage.Client.from_service_account_json(creds_path)
    else:
        print("ℹ️ Using Application Default Credentials (ADC)")
        return storage.Client()


def prune_gcs_snapshots(bucket_name, dry_run=True):
    """Deletes non-latest snapshot JSON and portfolio CSV files for each month in GCS bucket."""
    print(f"\n☁️ --- GCS Monthly Pruning (Bucket: '{bucket_name}') ---")
    client = get_gcs_client()
    if not client:
        return

    try:
        bucket = client.bucket(bucket_name)
        blobs = list(bucket.list_blobs())
    except Exception as e:
        print(f"❌ Failed to list GCS bucket: {e}")
        return

    gcs_date_regex = re.compile(r"^(?:snapshots|portfolios)/(\d{4}-\d{2}-\d{2})_")

    month_to_blobs = {}
    for blob in blobs:
        match = gcs_date_regex.match(blob.name)
        if match:
            date_str = match.group(1) # YYYY-MM-DD
            month_str = date_str[:7]   # YYYY-MM
            if month_str not in month_to_blobs:
                month_to_blobs[month_str] = {}
            if date_str not in month_to_blobs[month_str]:
                month_to_blobs[month_str][date_str] = []
            month_to_blobs[month_str][date_str].append(blob)

    to_keep_blobs = []
    to_delete = []

    for month_str, date_dict in sorted(month_to_blobs.items()):
        sorted_dates = sorted(date_dict.keys())
        latest_date = sorted_dates[-1]
        to_keep_blobs.extend(date_dict[latest_date])

        for d in sorted_dates[:-1]:
            for blob in date_dict[d]:
                to_delete.append((month_str, d, blob))

    print(f"📊 Found GCS files spanning {len(month_to_blobs)} month(s).")
    print(f"✅ GCS files to KEEP (for latest date of each month): {len(to_keep_blobs)}")
    print(f"🗑️ GCS files to DELETE (earlier dates in month): {len(to_delete)}")

    if to_delete:
        print("\nItems marked for deletion:")
        for month_str, date_str, blob in to_delete:
            print(f"  - [{month_str}] {blob.name} (Date: {date_str})")

    if dry_run:
        print("\n🔍 [DRY RUN] No GCS blobs were deleted.")
    else:
        print("\n🔥 Deleting non-latest GCS blobs...")
        deleted_count = 0
        for month_str, date_str, blob in to_delete:
            try:
                blob.delete()
                print(f"  Deleted: {blob.name}")
                deleted_count += 1
            except Exception as e:
                print(f"  ❌ Failed to delete {blob.name}: {e}")
        print(f"✅ Successfully deleted {deleted_count} GCS blob(s).")


def prune_sqlite_database(db_path, dry_run=True):
    """Deletes non-latest snapshot records for each month in SQLite DB."""
    print(f"\n🗄️ --- SQLite Database Monthly Pruning ('{db_path}') ---")
    if not os.path.exists(db_path):
        print(f"⚠️ Warning: Database file '{db_path}' not found. Skipping DB pruning.")
        return

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    try:
        cursor.execute("SELECT snapshotDate FROM portfolio_snapshot_headers ORDER BY snapshotDate ASC")
        rows = cursor.fetchall()
    except sqlite3.OperationalError as e:
        print(f"❌ Error querying database: {e}")
        conn.close()
        return

    if not rows:
        print("ℹ️ No portfolio snapshot headers found in database.")
        conn.close()
        return

    month_to_timestamps = {}
    for (ts,) in rows:
        dt = datetime.fromtimestamp(ts / 1000.0)
        month_str = dt.strftime("%Y-%m")
        date_str = dt.strftime("%Y-%m-%d")
        if month_str not in month_to_timestamps:
            month_to_timestamps[month_str] = []
        month_to_timestamps[month_str].append((ts, date_str))

    timestamps_to_keep = set()
    timestamps_to_delete = []

    for month_str, items in sorted(month_to_timestamps.items()):
        items.sort(key=lambda x: x[0])
        latest_ts, latest_date_str = items[-1]
        timestamps_to_keep.add(latest_ts)

        for ts, date_str in items[:-1]:
            timestamps_to_delete.append((month_str, date_str, ts))

    print(f"📊 Found database snapshots spanning {len(month_to_timestamps)} month(s).")
    print(f"✅ Snapshots to KEEP: {len(timestamps_to_keep)}")
    print(f"🗑️ Snapshots to DELETE: {len(timestamps_to_delete)}")

    if timestamps_to_delete:
        print("\nDatabase snapshot dates marked for deletion:")
        for month_str, date_str, ts in timestamps_to_delete:
            print(f"  - [{month_str}] Snapshot Date: {date_str} (timestamp: {ts})")

    if dry_run:
        print("\n🔍 [DRY RUN] No database records were deleted.")
    else:
        print("\n🔥 Deleting non-latest snapshot database rows...")
        ts_del_list = [ts for _, _, ts in timestamps_to_delete]
        placeholders = ",".join("?" * len(ts_del_list))

        cursor.execute(f"DELETE FROM portfolio_holdings WHERE snapshot_date IN ({placeholders})", ts_del_list)
        holdings_deleted = cursor.rowcount

        cursor.execute(f"DELETE FROM portfolio_snapshot_headers WHERE snapshotDate IN ({placeholders})", ts_del_list)
        headers_deleted = cursor.rowcount

        conn.commit()
        print(f"✅ Deleted {headers_deleted} header row(s) and {holdings_deleted} holding row(s).")

    conn.close()


def prune_local_files(data_dir, dry_run=True):
    """Deletes non-latest local snapshot and pipeline files in data_dir."""
    print(f"\n📂 --- Local Filesystem Monthly Pruning ('{data_dir}') ---")
    if not os.path.exists(data_dir):
        print(f"⚠️ Directory '{data_dir}' does not exist. Skipping local file pruning.")
        return

    local_date_regex = re.compile(r"^(\d{4}-\d{2}-\d{2})_.*")
    files = [f for f in os.listdir(data_dir) if local_date_regex.match(f)]
    if not files:
        print("ℹ️ No local date-prefixed files found.")
        return

    month_to_files = {}
    for filename in files:
        match = local_date_regex.match(filename)
        date_str = match.group(1)
        month_str = date_str[:7]
        if month_str not in month_to_files:
            month_to_files[month_str] = {}
        if date_str not in month_to_files[month_str]:
            month_to_files[month_str][date_str] = []
        month_to_files[month_str][date_str].append(filename)

    to_keep_files = []
    to_delete = []
    for month_str, date_dict in sorted(month_to_files.items()):
        sorted_dates = sorted(date_dict.keys())
        latest_date = sorted_dates[-1]
        to_keep_files.extend(date_dict[latest_date])

        for d in sorted_dates[:-1]:
            for filename in date_dict[d]:
                to_delete.append((month_str, d, filename))

    print(f"📊 Found local date-prefixed files spanning {len(month_to_files)} month(s).")
    print(f"✅ Local files to KEEP (for latest date of each month): {len(to_keep_files)}")
    print(f"🗑️ Files marked for deletion: {len(to_delete)}")

    if to_delete:
        print("\nLocal files marked for deletion:")
        for month_str, date_str, filename in to_delete:
            print(f"  - [{month_str}] {filename}")

    if dry_run:
        print("\n🔍 [DRY RUN] No local files were deleted.")
    else:
        print("\n🔥 Deleting non-latest local files...")
        deleted_count = 0
        for month_str, date_str, filename in to_delete:
            filepath = os.path.join(data_dir, filename)
            try:
                os.remove(filepath)
                print(f"  Deleted: {filename}")
                deleted_count += 1
            except Exception as e:
                print(f"  ❌ Failed to delete {filename}: {e}")
        print(f"✅ Successfully deleted {deleted_count} local file(s).")


def main():
    parser = argparse.ArgumentParser(description="Prune portfolio snapshot data to persist only the latest snapshot date for each month.")
    parser.add_argument("--apply", action="store_true", help="Execute deletion. If not set, runs in --dry-run mode.")
    parser.add_argument("--db-path", default=DEFAULT_DB_PATH, help=f"Path to SQLite database file (default: {DEFAULT_DB_PATH})")
    parser.add_argument("--gcs-bucket", default=DEFAULT_GCS_BUCKET, help=f"GCS bucket name (default: {DEFAULT_GCS_BUCKET})")
    parser.add_argument("--data-dir", default=DATA_DIR, help=f"Local snapshot directory (default: {DATA_DIR})")
    parser.add_argument("--skip-gcs", action="store_true", help="Skip GCS bucket pruning")
    parser.add_argument("--skip-db", action="store_true", help="Skip SQLite DB pruning")
    parser.add_argument("--skip-local", action="store_true", help="Skip local file pruning")

    args = parser.parse_args()
    dry_run = not args.apply

    if dry_run:
        print("⚠️ Running in DRY RUN mode. Pass --apply to perform actual deletion.")
    else:
        print("⚡ Mode: APPLY (Permanent deletion enabled)")

    if not args.skip_db:
        prune_sqlite_database(args.db_path, dry_run=dry_run)

    if not args.skip_gcs and args.gcs_bucket:
        prune_gcs_snapshots(args.gcs_bucket, dry_run=dry_run)

    if not args.skip_local and args.data_dir:
        prune_local_files(args.data_dir, dry_run=dry_run)

    print("\n🎉 Monthly portfolio pruning procedure completed!")


if __name__ == "__main__":
    main()
