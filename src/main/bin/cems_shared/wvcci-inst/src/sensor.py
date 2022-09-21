class Sensor:
    def __init__(self, name, period=None, data_version=''):
        """

        :type name: str
        :type period: Period
        :type data_version: str
        """
        self.name = name
        self.period = period
        self.data_version = data_version

    def get_name(self):
        """

        :rtype : str
        """
        return self.name

    def get_period(self):
        """

        :rtype : Period
        """
        return self.period

    def get_data_version(self):
        """

        :rtype : str
        """
        return self.data_version

    def __eq__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() == other.get_name() and self.get_period() == other.get_period()

    def __ne__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() != other.get_name() or self.get_period() != other.get_period()

    def __ge__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() >= other.get_name() or (
            self.get_name() == other.get_name() and self.get_period() >= other.get_period())

    def __gt__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() > other.get_name() or (
            self.get_name() == other.get_name() and self.get_period() > other.get_period())

    def __le__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() <= other.get_name() or (
            self.get_name() == other.get_name() and self.get_period() <= other.get_period())

    def __lt__(self, other):
        """

        :type other: Sensor
        :return: boolean
        """
        return self.get_name() < other.get_name() or (
            self.get_name() == other.get_name() and self.get_period() < other.get_period())

    def __hash__(self):
        return self.get_name().__hash__() * 31 + self.get_period().__hash__()
