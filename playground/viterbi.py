import math

states = ('EnterSite', 'LikelyBuyer', 'VisitingLikedPage', 'VisitingNonLikedPage', 'Exiting')

observations = ('exit', 'buy', 'visit_sports', 'visit_non_sports', 'search')

start_probability = {'EnterSite': 1, 'LikelyBuyer': 0, 'VisitingLikedPage': 0, 'VisitingNonLikedPage': 0, 'Exiting': 0}

transition_probability = {
    'EnterSite':             {'EnterSite': 0.0, 'LikelyBuyer': 0.0, 'VisitingLikedPage': 0.5, 'VisitingNonLikedPage': 0.4, 'Exiting': 0.1},
    'LikelyBuyer':           {'EnterSite': 0.0, 'LikelyBuyer': 0.0, 'VisitingLikedPage': 0.3, 'VisitingNonLikedPage': 0.1, 'Exiting': 0.6},
    'VisitingLikedPage':     {'EnterSite': 0.0, 'LikelyBuyer': 0.2, 'VisitingLikedPage': 0.3, 'VisitingNonLikedPage': 0.1, 'Exiting': 0.4},
    'VisitingNonLikedPage':  {'EnterSite': 0.0, 'LikelyBuyer': 0.1, 'VisitingLikedPage': 0.1, 'VisitingNonLikedPage': 0.2, 'Exiting': 0.6},
    'Exiting':               {'EnterSite': 0.0, 'LikelyBuyer': 0.0, 'VisitingLikedPage': 0.0, 'VisitingNonLikedPage': 0.0, 'Exiting': 1.0},
}

emission_probability = {
    'EnterSite':            {'exit': 0.1, 'buy': 0.0, 'visit_sports': 0.3, 'visit_non_sports': 0.1, 'search': 0.5},
    'LikelyBuyer':          {'exit': 0.1, 'buy': 0.7, 'visit_sports': 0.1, 'visit_non_sports': 0.1, 'search': 0.0},
    'VisitingLikedPage':    {'exit': 0.1, 'buy': 0.4, 'visit_sports': 0.2, 'visit_non_sports': 0.1, 'search': 0.2},
    'VisitingNonLikedPage': {'exit': 0.5, 'buy': 0.0, 'visit_sports': 0.1, 'visit_non_sports': 0.2, 'search': 0.2},
    'Exiting':              {'exit': 0.9, 'buy': 0.0, 'visit_sports': 0.0, 'visit_non_sports': 0.0, 'search': 0.1}
}

# from https://en.wikipedia.org/wiki/Viterbi_algorithm#Example
def viterbi(obs, states, start_p, trans_p, emit_p):
    v = [{}]
    path = {}

    # Initialize base cases (t == 0)
    for y in states:
        v[0][y] = start_p[y] * emit_p[y][obs[0]]
        path[y] = [y]

    # Run Viterbi for t > 0
    for t in range(1, len(obs)):
        v.append({})
        new_path = {}

        for y in states:
            (prob, state) = max((v[t - 1][y0] * trans_p[y0][y] * emit_p[y][obs[t]], y0) for y0 in states)
            v[t][y] = prob
            new_path[y] = path[state] + [y]

        # Don't need to remember the old paths
        path = new_path

    # Return the most likely sequence over the given time frame
    n = len(obs) - 1
    print(*dptable(v), sep='')
    (prob, state) = max((v[n][y], y) for y in states)
    return prob, path[state]


# Don't study this; it just prints a table of the steps.
def dptable(v):
    yield "    "
    yield " ".join(("%7d" % i) for i in range(len(v)))
    yield "\n"
    for y in v[0]:
        yield "%.5s: " % y
        yield " ".join("%.7s" % ("%f" % v[y]) for v in v)
        yield "\n"


def valid(start_p, trans_p, emit_p):
    assert sum(start_p.values()) == 1
    for p in trans_p:
        assert math.isclose(sum(trans_p[p].values()), 1)
    for p in emit_p:
        assert math.isclose(sum(emit_p[p].values()), 1)


def example():
    valid(start_probability, transition_probability, emission_probability)
    return viterbi(observations,
                   states,
                   start_probability,
                   transition_probability,
                   emission_probability)


print(example())
