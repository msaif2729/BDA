import math
import hashlib
import time


# ============================================================
# PART 1: BLOOM FILTER IMPLEMENTATION
# ============================================================

class BloomFilter:
    def __init__(self, expected_elements: int, false_positive_rate: float):
        self.n = expected_elements
        self.p = false_positive_rate

        # Optimal bit-array size
        self.m = int(
            -(self.n * math.log(self.p)) / (math.log(2) ** 2)
        )

        # Optimal number of hash functions
        self.k = int((self.m / self.n) * math.log(2))

        # Allocate bit array
        self.bit_array = [0] * self.m

        print(
            f"[Bloom Filter Initialized] Size (m): {self.m} bits | "
            f"Hash functions (k): {self.k}"
        )

    def _hashes(self, item: str):
        """Generate k hash positions using double hashing."""
        md5_hash = hashlib.md5(item.encode("utf-8")).hexdigest()

        # Split the MD5 digest into two 64-bit integers
        h1 = int(md5_hash[:16], 16)
        h2 = int(md5_hash[16:], 16)

        for i in range(self.k):
            yield (h1 + i * h2) % self.m

    def add(self, item: str):
        """Insert an element into the Bloom Filter."""
        for bit_index in self._hashes(item):
            self.bit_array[bit_index] = 1

    def contains(self, item: str) -> bool:
        """Check whether an element may be in the Bloom Filter."""
        return all(
            self.bit_array[bit_index] == 1
            for bit_index in self._hashes(item)
        )


# ============================================================
# PART 2: DGIM ALGORITHM IMPLEMENTATION
# ============================================================

class DGIMBucket:
    def __init__(self, size: int, timestamp: int):
        self.size = size                  # 1, 2, 4, 8, ...
        self.timestamp = timestamp        # Arrival position


class DGIM:
    def __init__(self, window_size: int):
        self.N = window_size
        self.current_time = 0
        # Buckets are ordered newest (index 0) to oldest
        self.buckets = []

    def update(self, bit: int):
        """Process one incoming bit from the stream."""
        if bit not in (0, 1):
            raise ValueError("Stream bits must be 0 or 1.")

        self.current_time += 1

        # Remove buckets older than the sliding window
        self.buckets = [
            bucket for bucket in self.buckets
            if (self.current_time - bucket.timestamp) <= self.N
        ]

        if bit == 1:
            # Create a new bucket of size 1
            new_bucket = DGIMBucket(
                size=1,
                timestamp=self.current_time
            )
            self.buckets.insert(0, new_bucket)

            # Merge buckets when necessary
            self._merge_buckets()

    def _merge_buckets(self):
        """Keep no more than two buckets of the same size."""
        idx = 0

        while idx < len(self.buckets) - 2:
            if (
                self.buckets[idx].size
                == self.buckets[idx + 1].size
                == self.buckets[idx + 2].size
            ):
                # Merge the two older buckets
                merged_size = self.buckets[idx + 1].size * 2
                merged_timestamp = self.buckets[idx + 1].timestamp

                del self.buckets[idx + 2]
                del self.buckets[idx + 1]

                self.buckets.insert(
                    idx + 1,
                    DGIMBucket(merged_size, merged_timestamp)
                )
            else:
                idx += 1

    def count_ones(self, k: int) -> int:
        """Estimate the number of 1s in the last k stream bits."""
        if k < 0:
            raise ValueError("Window query k cannot be negative.")

        if k > self.N:
            k = self.N

        if k == 0 or not self.buckets:
            return 0

        cutoff_time = self.current_time - k
        count = 0
        last_bucket = None

        for bucket in self.buckets:
            if bucket.timestamp > cutoff_time:
                count += bucket.size
                last_bucket = bucket
            else:
                # First bucket crossing the query-window boundary
                last_bucket = bucket
                break

        # Count half of the oldest overlapping bucket
        if last_bucket and last_bucket.timestamp <= cutoff_time:
            count += last_bucket.size // 2

        return count


# ============================================================
# EXPERIMENTAL RUN AND VALIDATION
# ============================================================

if __name__ == "__main__":

    print("=" * 64)
    print("EXPERIMENT 6: BLOOM FILTER & DGIM ALGORITHM FOR CYBERSECURITY")
    print("=" * 64)

    # --------------------------------------------------------
    # TEST BLOOM FILTER
    # --------------------------------------------------------

    print("\n>>> [1] INITIALIZING BLOOM FILTER FOR MALICIOUS URL LOOKUP")

    malicious_dataset = [
        "http://malware-download-site.ru/payload.exe",
        "http://phishing-login-portal-bank.com/secure",
        "http://ransomware-c2-server.net/beacon",
        "http://crypto-stealer-tracker.org/script.js",
        "http://trojan-delivery-domain.cn/update"
    ]

    bf = BloomFilter(
        expected_elements=100,
        false_positive_rate=0.05
    )

    # Insert known malicious URLs
    for url in malicious_dataset:
        bf.add(url)

    print(f"Added {len(malicious_dataset)} malicious URLs to Bloom Filter.\n")

    # Test URLs: True means known malicious; False means safe/unknown
    test_queries = [
        ("http://phishing-login-portal-bank.com/secure", True),
        ("https://google.com/search", False),
        ("http://ransomware-c2-server.net/beacon", True),
        ("https://github.com/torvalds/linux", False),
        ("http://free-gift-card-generator.xyz/claim", False)
    ]

    print("--- URL Query Results ---")

    false_positives = 0
    true_negatives = 0

    for url, is_malicious in test_queries:
        detected = bf.contains(url)

        status = (
            "POTENTIALLY MALICIOUS (Flagged)"
            if detected
            else "DEFINITELY NOT IN FILTER (Passed)"
        )

        print(f"URL: {url:<48} => {status}")

        if not is_malicious and detected:
            false_positives += 1
        elif not is_malicious and not detected:
            true_negatives += 1

    if false_positives + true_negatives > 0:
        empirical_fpr = false_positives / (false_positives + true_negatives)
        print(f"\nEmpirical false-positive rate on safe test URLs: "
              f"{empirical_fpr * 100:.2f}%")

    # --------------------------------------------------------
    # TEST DGIM ALGORITHM
    # --------------------------------------------------------

    print("\n" + "=" * 64)
    print(">>> [2] RUNNING DGIM ALGORITHM ON THREAT DETECTION STREAM")

    window_N = 32
    dgim = DGIM(window_size=window_N)

    # 1 = malicious request, 0 = safe request
    stream_data = [
        1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1,
        0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0,
        1, 1, 0, 0, 1, 0, 1, 1
    ]

    print(f"Window Size (N): {window_N} bits")
    print(f"Ingesting stream of {len(stream_data)} incoming packets/events...")

    for bit in stream_data:
        dgim.update(bit)

    # Query the last k events
    k_query = 20
    dgim_estimate = dgim.count_ones(k=k_query)

    # Calculate exact count for comparison
    actual_stream_slice = stream_data[-k_query:]
    ground_truth_count = sum(actual_stream_slice)

    absolute_error = abs(dgim_estimate - ground_truth_count)
    relative_error_pct = (
        (absolute_error / ground_truth_count) * 100
        if ground_truth_count else 0.0
    )

    print(f"\n--- Sliding Window Query (Last k = {k_query} events) ---")
    print(f"Stream Window Data       : {actual_stream_slice}")
    print(f"Actual Count of 1s       : {ground_truth_count}")
    print(f"DGIM Estimated Count     : {dgim_estimate}")
    print(f"Absolute Estimation Error: {absolute_error}")
    print(f"Relative Error           : {relative_error_pct:.2f}%")
    print("The standard DGIM estimate has a maximum relative error "
          "of up to 50%.")

    print("\nExperiment 6 completed.")
