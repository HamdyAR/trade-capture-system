import React from "react";
import Input from "../components/Input";
import Button from "../components/Button";
import api from "../utils/api";
import LoadingSpinner from "../components/LoadingSpinner";
import {Trade} from "../utils/tradeTypes";
import AGGridTable from "../components/AGGridTable";
import {getColDefFromResult, getRowDataFromData} from "../utils/agGridUtils";
import { ColDef, SelectionChangedEvent } from "ag-grid-community";


interface SearchCriteria{
    book?: string;
    counterparty?: string;
    trader?: string;
    tradeStatus: string;
    startDate?: string;
    endDate: string;
}


interface MultiCriteriaSearchProps{
    onTradeSelect: (trade: Trade) => void;
}

export const MultiCriteriaSearch: React.FC<MultiCriteriaSearchProps> = ({ onTradeSelect }) => {
    const [searchCriteria, setSearchCriteria] = React.useState<SearchCriteria>(
        {
            book: '',
            counterparty: '',
            trader: '',
            tradeStatus: '',
            startDate: '',
            endDate: ''
        }
    );

    //State for search results
    const [results, setResults] = React.useState<Trade[]>([]);
    const [loading, setLoading] = React.useState<boolean>(false);
    const [error, setError] = React.useState<string>('');
    const [hasSearched, setHasSearched] = React.useState<boolean>(false);

    //rsql
    const [isRsqlActive, setIsRsqlActive] = React.useState<boolean>(false);
    const [query, setQuery] = React.useState<string>('');

    //settlement
    const [isSettlementActive, setIsSettlementActive] = React.useState<boolean>(false);
    const [instructions, setInstructions] = React.useState<string>('');

    //Pagination state
    const [currentPage, setCurrentPage] = React.useState(0);
    const [totalPages, setTotalPages] = React.useState(0);
    const [totalTrades, setTotalTrades] = React.useState(0);
    const pageSize = 20;


    const handleInputChange = (field: keyof SearchCriteria, value: string) => {
        setSearchCriteria(prev => ({
            ...prev,
            [field]: value
        }))
    }
    
    const handleSearch = async (page: number = 0) => {
        setLoading(true);
        setError('');
        setHasSearched(true);
        setCurrentPage(page);

        try{
            const params = new URLSearchParams();

            if(isRsqlActive){
                if(!query || query.trim() === ''){
                    setError("RSQL query cannot be empty");
                    setLoading(false);
                }
                params.append('query', query);
            }
            if(isSettlementActive){
                if(!instructions || instructions.trim() === ''){
                    setError("Settlement query cannot be empty");
                    setLoading(false);
                }
                params.append('instructions', instructions);
            }
            else{
                if(searchCriteria.book) params.append('book', searchCriteria.book);
                if(searchCriteria.counterparty) params.append('counterparty', searchCriteria.counterparty);
                if(searchCriteria.trader) params.append('trader', searchCriteria.trader);
                if(searchCriteria.tradeStatus) params.append('tradeStatus', searchCriteria.tradeStatus);
                if(searchCriteria.startDate) params.append('startDate', searchCriteria.startDate);
                if(searchCriteria.endDate) params.append('endDate', searchCriteria.endDate);
            }

            //pagination params
            if(!isSettlementActive){
                params.append('page', page.toString());
                params.append('size', pageSize.toString());
                params.append('sortBy', 'tradeDate');
                params.append('sortDir', 'desc');
            }
            

            let endpoint = "";
            if(isRsqlActive){
               endpoint = '/trades/rsql';
            }
            if(isSettlementActive){
                endpoint = '/trades/search/settlement-instructions';
            }
            else{
                endpoint = 'trades/filter';
            }
            // const endpoint = isRsqlActive ? '/trades/rsql' : 'trades/filter';
            const response = await api.get(`${endpoint}?${params.toString()}`);

            if(response.status === 200){
                const returnedData = isSettlementActive ?  response.data : response.data.content ;

                 const mappedData = (returnedData as Trade[]).map((item: Trade) => {
                                const settlementField = Array.isArray(item.additionalFields)
                            ? item.additionalFields.find((f: any) =>f && typeof f === "object" && f.fieldName === "SETTLEMENT_INSTRUCTIONS")
                            : undefined;
                
                            const settlementInstructions = settlementField && settlementField.fieldValue ? settlementField.fieldValue : '';
                
                            return{
                                ...item,
                                settlementInstructions
                            }
                            })
                
                setResults(mappedData || []);
                setTotalPages(response.data.totalPages || 0);
                setTotalTrades(response.data.totalElements || 0)

                console.log(response.data.content);
            }
        }catch (err) {
            setError(err instanceof Error ? err.message: "An error occurred while searching");
            setResults([]);
        }
        finally{
            setLoading(false);
        }
    }
    

     const handleClearAll = () => {
        setSearchCriteria({
        book: '',
        counterparty: '',
        trader: '',
        tradeStatus: '',
        startDate: '',
        endDate: ''
        })
        setQuery('');
        setIsRsqlActive(false);
        setInstructions('');
        setIsSettlementActive(false);
        setResults([]);
        setError('');
        setHasSearched(false);
        setCurrentPage(0);
        setTotalPages(0);
        setTotalTrades(0);
    };

    const handleRowClick = (trade: Trade) => {
        onTradeSelect(trade);
    };

    const handlePageChange = (newPage: number) => {
        handleSearch(newPage);
    };

    return ( 
        <div className="p-6 bg-white rounded-lg"> 
            <div className="mb-4 p-3 ">
                
                <label htmlFor="rsql" className="flex items-center cursor-pointer">
                    <input
                   type="checkbox"
                   id="rsql"
                   checked={isRsqlActive}
                   onChange={(e) => setIsRsqlActive(e.target.checked)}
                   className="mr-4 cursor-pointer"
                   />
                   <span className="text-sm font-medium">
                    Use RSQL Query (for advanced users)
                   </span>
                </label>

                <label htmlFor="settlement" className="flex items-center cursor-pointer mt-2">
                    <input
                   type="checkbox"
                   id="settlement"
                   checked={isSettlementActive}
                   onChange={(e) => setIsSettlementActive(e.target.checked)}
                   className="mr-4 cursor-pointer"
                   />
                   <span className="text-sm font-medium">
                    Search by Settlement instructions
                   </span>
                </label>
                 
            </div>
             
           {isRsqlActive && (
               <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                        RSQL Query
                   </label>
                    <Input
                    type="text"
                    placeholder="Enter RSQL query"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    className="w-full"
                    />  
                    <div className="flex gap-2 justify-center mt-4">
                    <Button
                    type="button"
                    variant="primary"
                    onClick={() => handleSearch(0)}
                    className="cursor-pointer" 
                    >
                        Search
                    </Button>
                    <Button
                    type="button"
                    variant="primary"
                    onClick={handleClearAll}
                    className="!bg-gray-500 hover:!bg-gray-700 cursor-pointer"
                    >
                        Clear
                    </Button>
                </div>   
            </div>
           )}

            {isSettlementActive && (
               <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                        Search by Settlement Instructions
                   </label>
                    <Input
                    type="text"
                    placeholder="Enter settlement instruction text"
                    value={instructions}
                    onChange={(e) => setInstructions(e.target.value)}
                    className="w-full"
                    />  
                    <div className="flex gap-2 justify-center mt-4">
                    <Button
                    type="button"
                    variant="primary"
                    onClick={() => handleSearch(0)}
                    className="cursor-pointer" 
                    >
                        Search
                    </Button>
                    <Button
                    type="button"
                    variant="primary"
                    onClick={handleClearAll}
                    className="!bg-gray-500 hover:!bg-gray-700 cursor-pointer"
                    >
                        Clear
                    </Button>
                </div>   
            </div>
           )}




          {!isRsqlActive && !isSettlementActive && (
              <form onSubmit={(e) => {e.preventDefault(); handleSearch(0);}} className="space-y-4">
             <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                <div>
                   <label className="block text-sm font-medium text-gray-700 mb-1">
                       Book
                   </label>
                   <Input
                   type="text"
                   placeholder="Enter book name"
                   value={searchCriteria.book}
                   onChange={(e) => handleInputChange('book', e.target.value)}
                   className="w-full"
                   />
                </div>
                 <div>
                   <label className="block text-sm font-medium text-gray-700 mb-1">
                       Counterparty
                   </label>
                   <Input
                   type="text"
                   placeholder="Enter counterparty name"
                   value={searchCriteria.counterparty}
                   onChange={(e) => handleInputChange('counterparty', e.target.value)}
                   className="w-full"
                   />
                </div>
                <div>
                   <label className="block text-sm font-medium text-gray-700 mb-1">
                       Trade Status
                   </label>
                   <Input
                   type="text"
                   placeholder="Enter trade status"
                   value={searchCriteria.tradeStatus}
                   onChange={(e) => handleInputChange('tradeStatus', e.target.value)}
                   className="w-full"
                   />
                </div>
                <div>
                   <label className="block text-sm font-medium text-gray-700 mb-1">
                       Trader
                   </label>
                   <Input
                   type="text"
                   placeholder="Enter trader name"
                   value={searchCriteria.trader}
                   onChange={(e) => handleInputChange('trader', e.target.value)}
                   className="w-full"
                   />
                </div>
                <div>
                   <label className="block text-sm font-medium text-gray-700 mb-1">
                       Start Date
                   </label>
                   <Input
                   type="date"
                   placeholder="Enter start date"
                   value={searchCriteria.startDate}
                   onChange={(e) => handleInputChange('startDate', e.target.value)}
                   className="w-full"
                   />
                </div>
                <div>
                   <label className="block text-sm font-medium text-gray-700 mb-1">
                       End Date
                   </label>
                   <Input
                   type="date"
                   placeholder="Enter end date"
                   value={searchCriteria.endDate}
                   onChange={(e) => handleInputChange('endDate', e.target.value)}
                   className="w-full"
                   />
                </div>
             </div>
             <div className="flex gap-2 justify-center mt-4">
                <Button
                type="submit"
                variant="primary" 
                className="cursor-pointer" 
                >
                    Search
                </Button>
                <Button
                type="button"
                variant="primary"
                onClick={handleClearAll}
                className="!bg-gray-500 hover:!bg-gray-700 cursor-pointer"
                >
                    Clear
                </Button>
             </div>
           </form>
          )}  
                   

           {loading && (
            <div className="mt-6">
               <LoadingSpinner/>
            </div>    
           )}

           {error && 
            <div className="mt-6 p-4 bg-red-100 border-red-400 text-red-700 rounded">
                {error}
            </div>    
           }
           {hasSearched && results.length > 0 && (
            
             <div className="mt-6">
                {!isSettlementActive && 
                  <div className="flex justify-center items-center mb-3">
                    <h3 className="text-md font-semibold mr-4">
                        Search Results: ({totalTrades} {totalTrades === 1 ? 'trade' : 'trades'})
                    </h3>
                    <span className="text-sm text-gray-600">
                        Page {currentPage + 1} of {totalPages}
                    </span>
                </div>  
                
                }
                

                <AGGridTable
                columnDefs={getColDefFromResult(results) as ColDef[]}
                rowData={getRowDataFromData(results)}
                onSelectionChanged={(event: SelectionChangedEvent) => {
                    const selectedRows = event.api.getSelectedRows();
                    if(selectedRows && selectedRows.length > 0){
                        handleRowClick(selectedRows[0] as Trade)
                    }
                }}
                rowSelection={"single"}
                /> 

                {totalPages > 1 && (
                    <div className="flex justify-center items-center gap-2 mt-4">
                        <Button
                        variant="primary"
                        onClick={() => handlePageChange(currentPage - 1)}
                        disabled={currentPage === 0}
                        className="!bg-gray-700 hover:!bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            Previous
                        </Button>
                        <span className="text-sm px-4 py-2 text-gray-700">
                              Page {currentPage + 1} of {totalPages}
                        </span>
                        <Button
                        variant="primary"
                        onClick={() => handlePageChange(currentPage + 1)}
                        disabled={currentPage >= totalPages - 1}
                        className="disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            Next
                        </Button>
                    </div>    
                )} 
              </div>  
           )}
           {hasSearched && results.length === 0 && !error && (
            <div className="mt-10">
            <p className="text-md text-gray-600">No results found</p>
            </div>
           )}
        </div>
    )  
}