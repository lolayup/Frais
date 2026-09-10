#ifndef FRAIS_FUZZY_SEARCH_HPP
#define FRAIS_FUZZY_SEARCH_HPP

#include <string>
#include <vector>

namespace frais {

class FuzzySearch {
public:
    static bool search(const std::string& raw, const std::string& query, bool useFuzzy);
    static int levenshteinDistance(const std::string& s1, const std::string& s2);

private:
    static bool containsInOrder(const std::string& strA, const std::string& strB);
    static std::string toUpper(const std::string& str);
};

} // namespace frais

#endif // FRAIS_FUZZY_SEARCH_HPP
