import React from "react";
import Input from "../components/Input";
import Button from "../components/Button";
import api from "../utils/api";
import { SingleTradeModal} from "./SingleTradeModal";
import { getDefaultTrade } from "../utils/tradeUtils";
import userStore from "../stores/userStore";
import LoadingSpinner from "../components/LoadingSpinner";
import Snackbar from "../components/Snackbar";
import {observer} from "mobx-react-lite";
import {useQuery} from '@tanstack/react-query';
import staticStore from "../stores/staticStore";
import {Trade, TradeLeg} from "../utils/tradeTypes";
import { MultiCriteriaSearch } from "./MultiSearchCriteria";

export const TradeActionsModalPrac: React.FC = observer(() => {
    const [activeTab, setActiveTab] = React.useState<'quick' | 'advanced'>('quick');
    const [tradeId, setTradeId] = React.useState<string>("");
    const [snackBarOpen, setSnackbarOpen] = React.useState<boolean>(false);
    const [trade, setTrade] = React.useState<Trade | null>(null);
    const [loading, setLoading] = React.useState<boolean>(false);
    const [snackbarMessage, setSnackbarMessage] = React.useState<string>("");
    const [isLoadError, setIsLoadError] = React.useState<boolean>(false);
    const [modalKey, setModalKey] = React.useState(0);

    const {isSuccess, error} = useQuery({
        queryKey: ["staticValues"],
        queryFn: () => staticStore.fetchAllStaticValues(),
        refetchInterval: 30000,
        refetchOnWindowFocus: false,
    });

    React.useEffect(() => {
        if (isSuccess) {
            staticStore.isLoading = false;
            console.log("Static values loaded successfully");
        }
        if (error) {
            staticStore.error = (error).message || 'Unknown error';
        }
    }, [isSuccess, error]);

    const handleSearch = async (e: React.FormEvent) => {
        e.preventDefault();
        console.log("Searching for trade ID:", tradeId);
        setLoading(true)
        try {
            const tradeResponse = await api.get(`/trades/${tradeId}`);
            if (tradeResponse.status === 200) {
                const convertToDate = (val: string | undefined) => val ? new Date(val) : undefined;
                const tradeData = tradeResponse.data;
                const dateFields = [
                    'tradeDate',
                    'startDate',
                    'maturityDate',
                    'executionDate',
                    'lastTouchTimestamp',
                    'validityStartDate'
                ];

                const formatDateForInput = (date: Date | undefined) =>
                    date ? date.toISOString().slice(0, 10) : undefined;
                dateFields.forEach(field => {
                    if (tradeData[field]) {
                        const dateObj = convertToDate(tradeData[field]);
                        tradeData[field] = formatDateForInput(dateObj);
                    }
                });
                if (Array.isArray(tradeData.tradeLegs)) {
                    console.log(`Found ${tradeData.tradeLegs.length} trade legs in the response`);
                    tradeData.tradeLegs = tradeData.tradeLegs.map((leg: TradeLeg) => {
                        console.log("Processing leg:", leg);
                        return {
                            ...leg,
                            legId: leg.legId || '',
                            legType: leg.legType || '',
                            rate: leg.rate !== undefined ? leg.rate : '',
                            index: leg.index || '',
                        };
                    });
                } else {
                    console.warn("No trade legs found in the response!");
                    tradeData.tradeLegs = [];
                }
                setTrade(tradeData);
                setSnackbarOpen(true)
                setSnackbarMessage("Successfully fetched trade details");

            } else {
                console.error("Error fetching trade:", tradeResponse.statusText);
                setSnackbarMessage("Error fetching trade details: " + tradeResponse.statusText);
                setIsLoadError(true)
            }
        } catch (error) {
            console.error("Error fetching trade:", error);
            setIsLoadError(true);
            setSnackbarOpen(true);
            setSnackbarMessage("Error fetching trade details: " + (error instanceof Error ? error.message : "Unknown error"));
        } finally {
            setTimeout(() => {
                setSnackbarOpen(false);
                setSnackbarMessage("")
                setIsLoadError(false)
            }, 3000);
            setLoading(false)
            setTradeId("")
        }
    };
    const handleClearAll = () => {
        setTrade(null);
        setTradeId("");
        setSnackbarOpen(false);
        setSnackbarMessage("");
        setIsLoadError(false);
        setLoading(false);
    };
    const handleBookNew = () => {
        const defaultTrade = getDefaultTrade();
        console.log('DEBUG getDefaultTrade:', defaultTrade);
        setTrade(defaultTrade);
        setModalKey(prev => prev + 1);
    };
    const mode = userStore.authorization === "TRADER_SALES" || userStore.authorization === "MO" ? "edit" : "view";
    return (
        <div className={"flex flex-col rounded-lg drop-shadow-2xl mt-0 bg-indigo-50 w-full h-full"}>
          
          <div className="flex justify-center gap-x-16 border-b border-gray-300 bg-white rounded-t-lg">
            <button className={`px-6 py-3 font-medium hover:cursor-pointer transition-colors ${
                activeTab === 'quick' ? 'border-b-2 border-indigo-600 text-indigo-600'
                : 'text-gray-600 hover:text-indigo-500'
            }`}
            onClick={() => setActiveTab('quick')}
            >
              Quick Lookup
            </button>
            <button className={`px-6 py-3 font-medium hover:cursor-pointer transition-colors ${
                activeTab === 'advanced' ? 'border-b-2 border-indigo-600 text-indigo-600'
                : 'text-gray-600 hover:text-indigo-500'
            }`}
            onClick={() => setActiveTab('advanced')}
            >
              Advanced Search
            </button>
          </div>

          {activeTab === 'quick' && (
            <div>
                 <div className={"flex flex-row items-center justify-center p-4 h-fit w-fit gap-x-2 mb-2 mx-auto"}>
                <Input size={"sm"}
                       type={"search"}
                       required
                       placeholder={"Search by Trade ID"}
                       key={"trade-id"}
                       value={tradeId}
                       onChange={(e) => setTradeId(e.currentTarget.value)}
                       className={"bg-white h-fit w-fit"}/>
                <Button variant={"primary"} type={"button"} size={"sm"} onClick={handleSearch}
                        className={"w-fit h-fit"}>Search</Button>
                <Button variant={"primary"} type={"button"} size={"sm"} onClick={handleClearAll}
                        className={"w-fit h-fit !bg-gray-500 hover:!bg-gray-700"}>Clear</Button>
                { userStore.authorization === "TRADER_SALES" &&
                <Button variant={"primary"} type={"button"} size={"sm"} onClick={handleBookNew}
                        className={"w-fit h-fit"}>Book New</Button>
                }
            </div>
             </div>   
          )}

          {activeTab === 'advanced' && (
            <div className="p-6 text-center">
                {/* <p className="text-gray-600 text-lg">
                     Advanced search coming up
                </p> */}
                 <MultiCriteriaSearch
                 onTradeSelect={
                    (selectedTrade) => {
                        setTrade(selectedTrade);
                        setModalKey(prev => prev + 1)
            }}/>
            </div>
          )}  
            <div>
                {loading ? <LoadingSpinner/> : null}
                {trade && !loading && <SingleTradeModal key={modalKey} mode={mode} trade={trade} isOpen={!!trade} onClear={handleClearAll}/>}
            </div>
            <Snackbar open={snackBarOpen} message={snackbarMessage} onClose={() => setSnackbarOpen(false)}
                      type={isLoadError ? "error" : "success"}/>
        </div>
    )
})