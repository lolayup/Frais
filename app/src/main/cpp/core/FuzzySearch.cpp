#include "FuzzySearch.hpp"
#include <algorithm>
#include <cctype>

namespace frais {

bool FuzzySearch::search(const std::string& raw, const std::string& query, bool useFuzzy) {
    if (query.empty()) return true;
    if (raw.empty()) return false;

    std::string rawUpper = toUpper(raw);
    std::string queryUpper = toUpper(query);

    if (rawUpper.find(queryUpper) != std::string::npos) return true;
    if (!useFuzzy) return false;

    int diff = levenshteinDistance(rawUpper, queryUpper);
    return diff < static_cast<int>(rawUpper.length()) && containsInOrder(rawUpper, queryUpper);
}

int FuzzySearch::levenshteinDistance(const std::string& s1, const std::string& s2) {
    int m = s1.length();
    int n = s2.length();

    std::vector<int> v0(n + 1);
    std::vector<int> v1(n + 1);

    for (int i = 0; i <= n; i++) v0[i] = i;

    for (int i = 0; i < m; i++) {
        v1[0] = i + 1;
        for (int j = 0; j < n; j++) {
            int cost = (s1[i] == s2[j]) ? 0 : 1;
            v1[j + 1] = std::min({v1[j] + 1, v0[j + 1] + 1, v0[j] + cost});
        }
        v0 = v1;
    }

    return v0[n];
}

bool FuzzySearch::containsInOrder(const std::string& strA, const std::string& strB) {
    size_t indexA = 0;
    for (char charB : strB) {
        size_t foundIndex = strA.find(charB, indexA);
        if (foundIndex == std::string::npos) return false;
        indexA = foundIndex + 1;
    }
    return true;
}

std::string FuzzySearch::toUpper(const std::string& str) {
    std::string result = str;
    std::transform(result.begin(), result.end(), result.begin(), [](unsigned char c){ return std::toupper(c); });
    return result;
}

} // namespace frais
