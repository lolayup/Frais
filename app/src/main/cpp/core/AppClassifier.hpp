#ifndef FRAIS_APP_CLASSIFIER_HPP
#define FRAIS_APP_CLASSIFIER_HPP

#include <string>
#include <vector>

namespace frais {

class AppClassifier {
public:
    static std::vector<int> classify(const std::string& packageName, const std::string& label, int category);
    static bool isGame(const std::string& packageName, int category, bool hasGameMetadata);

private:
    static bool containsAny(const std::string& target, const std::vector<std::string>& keywords);
};

} // namespace frais

#endif // FRAIS_APP_CLASSIFIER_HPP
